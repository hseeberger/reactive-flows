var reactiveFlowsControllers = angular.module('reactiveFlowsControllers', ['reactiveFlowsServices']);

reactiveFlowsControllers.controller('HomeCtrl', ['$scope', 'Flow', 'Post', function($scope, Flow, Post) {

    $scope.flows = [];

    $scope.currentFlowName = null;

    $scope.currentFlowLabel = null;

    $scope.posts = [];

    $scope.shouldShowForm = false;

    $scope.post = new Post({'text': ''});

    $scope.flowBtnClass = function(name) {
        return ($scope.currentFlowName === name) ? 'btn-primary' : 'btn-info';
    };

    $scope.switchCurrentFlow = function(name) {
        if ($scope.currentFlowName !== name) {
            console.log('Switching to flow ' + name);
            $scope.currentFlowName = name;
            $scope.currentFlowLabel = $scope.flows.find(function(flow) {
                return flow.name === name;
            }).label;
            $scope.shouldShowForm = true;
            var posts = Post.query({'name': name}, function() {
                console.log('Received ' + posts.length + ' posts for flow ' + name);
                $scope.posts = posts.map(function(post) {
                    post.time = Date.parse(post.time);
                    return post;
                });
            });
        }
    };

    $scope.sendPost = function() {
        $scope.post.$save({'name': $scope.currentFlowName});
        $scope.post = new Post({'text': ''});
    };

    // Initialize flows
    var flows = Flow.query(function() {
        console.log('Received ' + flows.length + ' flows from server');
        if (flows.length > 0) {
            $scope.flows = flows;
            $scope.switchCurrentFlow(flows[0].name);
        }
    });

    // SSE for flows-events
    var flowsEventsSource = new EventSource('/flows-events');
    flowsEventsSource.addEventListener(
        'added',
        function(event) {
            $scope.$apply(function() {
                var flow = JSON.parse(event.data);
                console.log('Received flow added event for flow ' + flow.name);
                $scope.flows.push(flow);
                if ($scope.currentFlowName === null)
                    $scope.switchCurrentFlow(flow.name);
            });
        },
        false
    );
    flowsEventsSource.addEventListener(
        'removed',
        function(event) {
            $scope.$apply(function() {
                console.log('Received flow removed event for flow ' + event.data);
                $scope.flows = $scope.flows.filter(function(flow) {
                    return flow.name !== event.data;
                });
                if ($scope.flows.length > 0)
                    if ($scope.currentFlowName === event.data)
                        $scope.switchCurrentFlow($scope.flows[0].name);
                    else {
                        $scope.currentFlowName = null;
                        $scope.currentFlowLabel = null;
                        $scope.shouldShowForm = false;
                        $scope.Posts = [];
                    }
            });
        },
        false
    );

    // SSE for flow-events
    var flowEventsSource = new EventSource('/flow-events');
    flowEventsSource.addEventListener(
        'added',
        function(event) {
            $scope.$apply(function() {
                var postAdded = JSON.parse(event.data);
                console.log('Received post added event for flow ' + postAdded.name);
                if ($scope.currentFlowName === postAdded.name) {
                    var post = postAdded.post;
                    post.time = Date.parse(post.time);
                    $scope.posts.push(post);
                }
            });
        },
        false
    );
}]);
