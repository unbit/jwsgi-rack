jwsgi-rack
==========

A bridge between Rack and JWSGI, allows running Rack applications on JRUBY via uWSGI


Install
=======

Be sure to have at least uWSGI 1.9.12, and build it with JWSGI support

```
UWSGI_PROFILE=jwsgi make
```

Install jruby in your system

Finally build the jwsgi-rack java class

```
javac -cp <path_to_jruby.jar> jwsgi_rack.java
```

Running
=======

```ini
[uwsgi]
; bind to http port
http-socket = :9090
http-socket-modifier1 = 8

; add jar (or directories) to the java CLASSPATH
jvm-classpath = <path_to_uwsgi.jar>
jvm-classpath = <path_to_jruby.jar>

; set jruby options
jvm-opt = -Djruby.home=/Library/Frameworks/JRuby.framework/Versions/Current
jvm-opt = -Djruby.lib=/Library/Frameworks/JRuby.framework/Versions/Current/lib

; load the adapter
jwsgi = jwsgi_rack:application
; pass it the path of a config.ru file
jwsgi-rack = config.ru
```
