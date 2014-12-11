var reactiveFlowsControllers = angular.module('reactiveFlowsControllers', ['reactiveFlowsServices']);

reactiveFlowsControllers.controller('HomeCtrl', ['$scope', 'Flow', 'Message', function ($scope, Flow, Message) {

    $scope.flows = [];

    $scope.currentFlowName = null;

    $scope.currentFlowLabel = null;

    $scope.messages = [];

    $scope.shouldShowForm = false;

    $scope.message = new Message({'text': ''});

    $scope.flowBtnClass = function (name) {
        return ($scope.currentFlowName == name) ? 'btn-primary' : 'btn-info';
    };

    $scope.switchCurrentFlow = function (name) {
        if ($scope.currentFlowName != name) {
            console.log("Switching to flow " + name);
            $scope.currentFlowName = name;
            $scope.currentFlowLabel = $scope.flows.find(function (flow) { return flow.name == name;}).label;
            $scope.shouldShowForm = true;
            var messages = Message.query({'flowName': name}, function () {
                console.log('Received ' + messages.length + ' messages for flow ' + name);
                $scope.messages = messages;
            });
        }
    };

    $scope.sendMessage = function () {
        $scope.message.$save({'flowName': $scope.currentFlowName});
        $scope.message = new Message({'text': ''});
    };

    // Initialize flows
    var flows = Flow.query(function () {
        console.log('Received ' + flows.length + ' flows from server');
        if (flows.length > 0) {
            $scope.flows = flows;
            $scope.switchCurrentFlow(flows[0].name);
        }
    });

    // SSE for flows
    var flowSource = new EventSource('/flows?events');
    flowSource.addEventListener(
        'added',
        function (event) {
            $scope.$apply(function () {
                var flow = JSON.parse(event.data);
                console.log('Received flow added event for flow ' + flow.name);
                $scope.flows.push(flow);
                if ($scope.currentFlowName == null) {
                    $scope.switchCurrentFlow(flow.name);
                }
            });
        },
        false
    );
    flowSource.addEventListener(
        'removed',
        function (event) {
            $scope.$apply(function () {
                console.log('Received flow removed event for flow ' + event.data);
                $scope.flows = $scope.flows.filter(function (flow) {
                    return flow.name != event.data;
                });
                if ($scope.flows.length > 0) {
                    if ($scope.currentFlowName == event.data) {
                        $scope.switchCurrentFlow($scope.flows[0].name);
                    }
                } else {
                    $scope.currentFlowName = null;
                    $scope.currentFlowLabel = null;
                    $scope.shouldShowForm = false;
                    $scope.messages = [];
                }
            });
        },
        false
    );

    // SSE for messages
    var messageSource = new EventSource('/messages');
    messageSource.addEventListener(
        'added',
        function (event) {
            $scope.$apply(function () {
                var messageAdded = JSON.parse(event.data);
                console.log('Received message added event for flow ' + messageAdded.flowName);
                if ($scope.currentFlowName == messageAdded.flowName) {
                    $scope.messages.push(messageAdded.message);
                }
            });
        },
        false
    );
}]);
