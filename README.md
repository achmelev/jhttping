# Description

Jhttping is a java utility to measure the response times of a web service. It's partly inspired by httping. 
Jhttping shows how long it takes to connect to an url to retriev the first part of the response and then the rest.

# Synopsys

    java -jar jhttping-<version>.jar [options]

# Options

    -b,--bufsize <arg>    Read buffer size to use. (in bytes, default is
                           8192)
     -c,--count <arg>      How many probes to send before exiting.
     -d,--data <arg>       Request body to send (only for POST requests)
     -g,--url <arg>        This selects the url to probe. E.g.:
                           http://localhost/
     -H,--headers <arg>    Header lines to send. Separate multiple values with
                           a space
     -i,--interval <arg>   How many seconds to sleep between every probe sent.
     -I,--agent <arg>      User-Agent to send to the server.(instead of
                           'JHTTPing <version>')
     -m,--method <arg>     HTTP method to use. Allowed values: GET, POST,
                           HEAD. Default is GET
