var reactiveFlowsControllers = angular.module('reactiveFlowsControllers', ['reactiveFlowsServices']);

reactiveFlowsControllers.controller('HomeCtrl', ['$scope', 'Flow', 'Message', function($scope, Flow, Message) {

    $scope.flows = [];

    $scope.currentFlowName = null;

    $scope.currentFlowLabel = null;

    $scope.messages = [];

    $scope.shouldShowForm = false;

    $scope.message = new Message({'text': ''});

    $scope.flowBtnClass = function(name) {
        return ($scope.currentFlowName == name) ? 'btn-primary' : 'btn-info';
    };

    $scope.switchCurrentFlow = function(name) {
        if ($scope.currentFlowName != name) {
            console.log('Switching to flow ' + name);
            $scope.currentFlowName = name;
            $scope.currentFlowLabel = $scope.flows.find(function(flow) {
                return flow.name == name;
            }).label;
            $scope.shouldShowForm = true;
            var messages = Message.query({'flowName': name}, function() {
                console.log('Received ' + messages.length + ' messages for flow ' + name);
                $scope.messages = messages;
            });
        }
    };

    $scope.sendMessage = function() {
        $scope.message.$save({'flowName': $scope.currentFlowName});
        $scope.message = new Message({'text': ''});
    };

    // Initialize flows
    var flows = Flow.query(function() {
        console.log('Received ' + flows.length + ' flows from server');
        if (flows.length > 0) {
            $scope.flows = flows;
            $scope.switchCurrentFlow(flows[0].name);
        }
    });
}]);
