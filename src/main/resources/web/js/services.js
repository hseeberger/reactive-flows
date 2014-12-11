var reactiveFlowsServices = angular.module('reactiveFlowsServices', ['ngResource']);

reactiveFlowsServices.factory('Flow', function ($resource) {
    return $resource('flows');
});
