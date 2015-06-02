var app = angular.module('crm', [
    'ngCookies',
    'ngResource',
    'ngSanitize',
    'ngRoute'
]);
 
app.config(function ($routeProvider) {
    $routeProvider.when('/', {
        templateUrl: 'views/phonebook-list.html',
        controller: 'EmployeeListCtrl'
    });
});

app.controller('EmployeeListCtrl', function ($scope, $http) {
	
	loaddata($scope,$http);
	
	
	$scope.connect = function() {
		$scope.alerts=[];
		$http.post("rest/connect?profile=" + encodeURIComponent($scope.connectionProfile) 
				+ "&username=" + encodeURIComponent($scope.username)
				+ "&password=" + encodeURIComponent($scope.password)
		 ).success(function (data, status, headers, config) {
			 loaddata($scope,$http);
		}).error(function (data, status, headers, config) {
			if(status==403) {
				$scope.alerts=[{type: 'warning', message: '<strong>Failed to connect because of authentication error</strong>'}]
			} else {
				$scope.alerts=[{type: 'warning', message: '<strong>Failed to connect</strong>, status=' + status + ', message=' + data}]
			}		});
		
		$scope.dismiss();
	};
	
	$scope.disconnect = function() {
		$scope.alerts=[];
		$http.post("rest/disconnect").success(function (data, status, headers, config) {
			loaddata($scope,$http);
		 });
	};
	
	$scope.generate = function() {
		$http.get('rest/generate-data').success(
				function(data) {
					loaddata($scope,$http);
				}
		).error(function (data, status,headers) {
			if(status==403) {
				$scope.alerts=[{type: 'warning', message: '<strong>User does not have priviliges to write to the cache</strong>'}]
			} else {
				$scope.alerts=[{type: 'warning', message: '<strong>Server responsed with an error</strong>, status=' + status + ', message=' + data}]
			}
	        console.log('Error data ' + data);
			console.log('Error data.message ' + data.message);
			console.log('Error status ' + status);
	    });		
	};
	$scope.clear = function() {
		$http.get('rest/clear').success(function(data) {
			loaddata($scope,$http);
		}).error(function (data, status,headers) {
			if(status==403) {
				$scope.alerts=[{type: 'warning', message: '<strong>User does not have priviliges to clear the cache</strong>'}]
			} else {
				$scope.alerts=[{type: 'warning', message: '<strong>Server responsed with an error</strong>, status=' + status + ', message=' + data}]
			}
	        console.log('Error data ' + data);
			console.log('Error data.message ' + data.message);
			console.log('Error status ' + status);
	    });	
	};
	$scope.filter = function() {
		var value = $scope.filter.value;
    	if(value.length > 0) {
			$http.get('rest/filter/' + value).success(function (data) {
	            $scope.persons = data;
	        }).error(function (data, status) {
	            console.log('Error ' + data);
	        });
		} else {
			loaddata($scope,$http);
		}
	};
});

app.directive('closableModal', function() {
   return {
     restrict: 'A',
     link: function(scope, element, attr) {
       scope.dismiss = function() {
           element.modal('hide');
       };
     }
   } 
});

var loaddata = function ($scope, $http) {
	
	$http.get('rest/connectiondetails').success(function (data) {
		$scope.connectiondetails  = data;
		if($scope.connectiondetails.connected) {
			$http.get('rest/persons').success(function (data) {
				$scope.persons  = data;
		    }).error(function (data, status,headers) {
				if(status==503) {
					$scope.alerts=[{type: 'danger', message: '<strong>Back-end service in unavailable</strong>'}]
				} else {
					$scope.alerts=[{type: 'warning', message: '<strong>Server responsed with an error</strong>, status=' + status + ', message=' + data}]
				}
		        console.log('Error data ' + data);
				console.log('Error data.message ' + data.message);
				console.log('Error status ' + status);
		    });
		}
		
    }).error(function (data, status,headers) {
    	$scope.alerts=[{type: 'danger', message: '<strong>Failed to get connection details!</strong>'}]
    	console.log('Failed to get connection details!');
    	console.log('Error data: ' + data);
    	console.log('Error status: ' + status);
		console.log('Error data.message: ' + data.message);
		
    });
};