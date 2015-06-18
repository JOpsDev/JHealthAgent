# JHealthAgent
Monitor your Java servers

Add to any JVM process as a startup paramter like this

-javaagent:/path/to/jhealthagent.jar=port=5566,path=/path/to/health.log

You can use either port or path or both:

-javaagent:/path/to/jhealthagent.jar=path=/path/to/health.log

-javaagent:/path/to/jhealthagent.jar=port=5566

If a port is specified it will be opened for remote Nagios checks (beware of the security implications)
If a path is specified a file will be generated an updated with a one line status every minute.
The file can be erased after processing for example with a HP OpenView agent.

For integrating into Nagios use the check_mbean_collector plugin from the jboss2nagios project: http://jboss2nagios.cvs.sourceforge.net/viewvc/jboss2nagios/jboss2nagios/plugin/
