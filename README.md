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

Install jruby in your system (only version 1.7.4 has been tested)

Finally build the jwsgi-rack java class

```
javac -cp <path_to_jruby.jar>:<path_to_uwsgi.jar> jwsgi_rack.java
```

and make a handy jar:

```
jar cvf jwsgi_rack.jar *.class
```


Running
=======

```ini
[uwsgi]
; bind to http port
http-socket = :9090
; automatically route requests to the jvm plugin
http-socket-modifier1 = 8

; spawn 8 jvm threads
threads = 8

; enable post-buffering to allow rewind of request body (as required by rack specs)
post-buffering = 8192

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

Notes
=====

Virtual options cannot be set from command line. You have to use -S

```
uwsgi -S jwsgi-rack=config.ru -S jwsgi-rack-bundler=true ...
```

The jvm uWSGI build system tries to search for jvm/jni installation in well-known paths. If you have installed java in non-standard paths,
check the official documentation on: http://uwsgi-docs.readthedocs.org/en/latest/JVM.html

If you use the "gold" linker instead of the classic "ld", the LD_RUN_PATH environment variable will not be honoured. That means
you need to set the path of libjvm.so with LD_LIBRARY_PATH when you run the "uwsgi" command
