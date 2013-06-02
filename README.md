jwsgi-rack
==========

A bridge between Rack and JWSGI, allows running Rack applications on JRUBY via uWSGI

It is the first attempt to heavily test the possibility of the JVM plugin in uWSGI

Install
=======

Be sure to have at least uWSGI 1.9.12 (if it is still not released use the code from github), and build it with JWSGI support

(you need python and java/jni development headers)

```
UWSGI_PROFILE=jwsgi make
```

Install jruby in your system

Finally build the jwsgi-rack java class

```
javac -cp <path_to_jruby.jar>:<path_to_uwsgi.jar> jwsgi_rack.java
```

and made a handy jar:

```
jar cvf jwsgi_rack.jar *.class
```


Running
=======

```ini
[uwsgi]
; bind to http port
http-socket = :9090
http-socket-modifier1 = 8

; spawn 8 jvm threads
threads = 8

; add jar (or directories) to the java CLASSPATH
jvm-classpath = <path_to_uwsgi.jar>
jvm-classpath = <path_to_jruby.jar>
jvm-classpath = <path_to_jwsgi_rack.jar>

; set jruby options (this is an OSX installation)
jvm-opt = -Djruby.home=/Library/Frameworks/JRuby.framework/Versions/Current
jvm-opt = -Djruby.lib=/Library/Frameworks/JRuby.framework/Versions/Current/lib

; load the adapter
jwsgi = jwsgi_rack:application
; pass it the path of a config.ru file (this is a virtual option)
jwsgi-rack = config.ru
; decomment it for enabling bundler
; jwsgi-rack-bundler = true
```
