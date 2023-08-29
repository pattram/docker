# README for integration_test

## Configuration

If you want to test a server other than localhost,
create a file src/test/resources/test.properties (or copy
src/test/resources/default.properties) and set the value of 

~~~ 
hostname = 
~~~

to the name of the server to test


## Running the tests

To run the tests, use this command:

~~~
mvn test
~~~
