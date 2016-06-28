var reactiveFlowsServices = angular.module('reactiveFlowsServices', ['ngResource']);

reactiveFlowsServices.factory('Flow', function($resource) {
    return $resource('flows');
});

reactiveFlowsServices.factory('Message', function ($resource) {
    return $resource('flows/:name/messages', {count:3});
});
