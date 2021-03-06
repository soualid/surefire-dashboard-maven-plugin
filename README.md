# surefire-dashboard-maven-plugin

A simple maven plugin to generate a synthetic view of your project unit tests status.

I mainly created it because we needed a way to detect a newly broken test by someone in
our team in a blink by using a simple dashboard screen which is auto-refreshed somewhere in
our office.

![screenshot](https://cloud.githubusercontent.com/assets/1830223/14560237/847eaac2-030e-11e6-89c8-e81145c03057.png)

## Basic usage

```
<reporting>
    <plugins>
        <plugin>
            <groupId>net.oualid.maven.plugins</groupId>
            <artifactId>surefire-dashboard-plugin</artifactId>
            <version>0.1</version>
        </plugin>
    </plugins>
</reporting>
```

## Configuration

`maxTestsPerLine` Maximum boxes to show per line (default : 5).
`webservicesToCallOnError` (since `0.2-SNAPSHOT`)  Webservices to call when a test fail (you can use this to send notifications when a test fail),
the `{{testname}}` string will be replaced by the name of the failing test. Example :

```
<webservicesToCallOnError>
    <param><![CDATA[https://www.example.com/http2sms.cgi?message=Issue on unit test : {{testname}} !]]></param>
</webservicesToCallOnError>
```
