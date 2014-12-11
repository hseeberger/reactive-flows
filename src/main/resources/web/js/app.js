var reactiveFlows = angular.module('reactiveFlows', ['reactiveFlowsControllers', 'ngRoute']);

reactiveFlows.config(['$routeProvider', function ($routeProvider) {
    $routeProvider
        .when('/', {templateUrl: 'partials/home.html', controller: 'HomeCtrl'})
        .otherwise({redirectTo: '/'});
}]);
