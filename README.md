[liblouis-inspector][]
======================

A web-based user interface for *visualizing* [liblouis][] translation tables,
for making it easier to write, understand and debug complex tables. The
application is using the [Play!][play] Framework.

Installation
------------

+ Download the source code: `git clone https://github.com/bertfrees/liblouis-inspector.git`
+ Download and install the [Play!][play] Framework 2.0.2
+ Download and install liblouis. You need a specific version:
  - `git clone https://github.com/bertfrees/liblouis.git`
  - `cd liblouis`
  - `git checkout introspection`
  - `./autogen.sh`
  - `./configure`
  - `make`
  - `sudo make install`
+ Create a new mySQL database: `mysqladmin create liblouis-inspector
  -u <your_username> -p`
+ Configure the database connection in `conf/application.conf`:
  - db.default.driver="com.mysql.jdbc.Driver"
  - db.default.url="jdbc:mysql://127.0.0.1/liblouis-inspector"
  - db.default.user="your_username"
  - db.default.password="your_password"

Running
-------

+ `cd liblouis-inspector`
+ `/path/to/play-2.0.2/play start`
+ By default the application will run on http://localhost:9000/

Authors
-------

+ [Bert Frees](http://github.com/bertfrees)

[liblouis-inspector]: http://github.com/bertfrees/liblouis-inspector
[liblouis]: http://code.google.com/p/liblouis
[play]: http://playframework.org
