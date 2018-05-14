# JHealthAgent
Monitor your Java servers

## Installation
Add to any JVM process as a startup paramter like this

-javaagent:/path/to/jhealthagent.jar=port=5566,path=/path/to/health.log

## Configuration
You can use either port or path or both:

-javaagent:/path/to/jhealthagent.jar=path=/path/to/health.log

If a path is specified a file will be generated and updated with a one line status every minute.
The file can be erased after processing for example with a HP OpenView agent.

-javaagent:/path/to/jhealthagent.jar=port=5566

If a port is specified it will be opened for remote Nagios checks (beware of the security implications)

-javaagent:/path/to/jhealthagent.jar=port=5566,delay=10000

Because some software as well known application servers don't like it if a MBean server is created before they do, JHealthAgent initially delays it's operation for 5 seconds before accessing the MBean server an registering to the GC events. If you need that delay to be shorter or need to increase it on a slow or overloaded machine you may do so. During the initial delay GC events will not be counted.

### Optional config file

The location of the config file can be specified by the config property, e.g. like this:

 -javaagent:/path/to/jhealthagent.jar=path=/tmp/health.log,config=/path/to/jhealth.conf
 
The config file has a standard Java properties file format. The following is an example of the contents:
 
jhealth.format=$TIME{dd.MM.yyyy HH:mm:ss};$SYSPROP{jboss.node.name};$jhealth:type=YoungGC{count-2};$jhealth:type=TenuredGC{count-2};$java.lang:type=Threading{ThreadCount}

All dynamic values have the $domain{attribute} format. This can be an MBean domain and attribute. The TIME domain lets you specify the timecode according to java.text.SimpleDateFormat. jhealth:type=YoungGC and jhealth:type=TenuredGC are built into JHealthAgent up to now and will be converted to real MBeans in a later release.

jhealth.path=/tmp/health.log

The path to write the health file to.

jhealth.port=5678

The port to open e.g. for Nagios/Incinga integration.

jhealth.delay=1000

The inital delay before the internal GC logic starts taking the GC events into account. This helps raising alarms because the server is still in a startup phase in which it is not unusal that the Heap gets resized multiple times and GC events happen very frequently.

### Using System Properties
If you don't want to specify either port or path in the javaagent-option you can also use System Properties, which can be passed with the -D option to the JVM.

-Djhealth.path=/path/to/health.log

-Djhealth.port=5566

If both, the System Property and the javaagent option is used at the same time for one setting then the javaagent option overrides the System Property value.


## Nagios Plugin

For integrating into Nagios use the check_mbean_collector plugin from the jboss2nagios project: http://jboss2nagios.cvs.sourceforge.net/viewvc/jboss2nagios/jboss2nagios/plugin/
