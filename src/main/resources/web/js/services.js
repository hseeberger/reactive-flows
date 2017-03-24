var reactiveFlowsServices = angular.module('reactiveFlowsServices', ['ngResource']);

reactiveFlowsServices.factory('Flow', function($resource) {
    return $resource('flows');
});

reactiveFlowsServices.factory('Post', function ($resource) {
    return $resource('flows/:name/posts', {count:99});
});
