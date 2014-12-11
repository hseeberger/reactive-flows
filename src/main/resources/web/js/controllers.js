var reactiveFlowsControllers = angular.module('reactiveFlowsControllers', []);

reactiveFlowsControllers.controller('HomeCtrl', ['$scope', function ($scope) {

    $scope.flows = [
        {name: 'akka', label: 'Akka'},
        {name: 'angularjs', label: 'AngularJS'}
    ];

    $scope.currentFlowName = 'akka';

    $scope.currentFlowLabel = 'Akka';

    $scope.messages = [
        {text: 'Akka rocks!', dateTime: '2014-01-02T03:04:05.678'}
    ];

    $scope.shouldShowForm = true;

    $scope.flowBtnClass = function (name) {
        return ($scope.currentFlowName == name) ? 'btn-primary' : 'btn-info';
    };

    $scope.switchCurrentFlow = function (name) {
        if ($scope.currentFlowName != name) {
            console.log('Switching to flow ' + name);
            alert('TODO: Missing implementation!');
        }
    };

    $scope.sendMessage = function () {
        alert('TODO: Missing implementation!');
    };
}]);
