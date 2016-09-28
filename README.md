# fod-uploader
Java Utility for uploading packages to FoD

## Setup

The FoD-Uploader is configured to build a fat jar with the Gradle Shadow plugin as the default gradle task.

To compile, simply use the gradlew or gradlew.bat depending on your operating system.

```
.\gradlew.bat
```

If you are behind the HPE firewall, you will need to configure gradles's proxy settings in:

*/<user-directory>/.gradle/gradle.properties*

```
systemProp.http.proxyHost=web-proxy.atl.hp.com
systemProp.http.proxyPort=8080

systemProp.https.proxyHost=web-proxy.atl.hp.com
systemProp.https.proxyPort=8080

```


## Usage

### Current
The command line arguments have been completely reworked for 5.3. Arguments are now named and can be in any order: 

```
java -jar FodUpload.jar -u <url> -z <file> [-a <1|2>] -uc <username> <password> | -ac <key> <secret>  
    [-h] [-I <minutes>] [-p <1|2>] [-P <proxyUrl> <username> <password> <ntDomain> <ntWorkstation>] 
    [-s <true|false>] [-v]
```
Each option has a short and long name:

Short Name | Long Name          | Required? | Description                                                      
---------- | ------------------ |:---------:| --------------------------------------------------------
 -u        | -bsiUrl            | Yes       | Build server url                                                 
 -z        | -zipLocation       | Yes       | Location of scan                                                 
 -ac       | -apiCredentials    | Yes*      | Api credentials                                                  
 -uc       | -userCredentials   | Yes*      | User login credentials                                           
 -a        | -auditPreferenceId | No        | False positive audit type (Manual = 1, Automated = 2)            
 -p        | -scanPreferenceId  | No        | Scan mode (Standard = 1, Express = 2)                            
 -I        | -pollingInterval   | No        | Interval between checking scan status in minutes                 
 -P        | -proxy             | No        | Credentials for accessing the proxy                   
 -s        | -runSonatypeScan   | No        | Whether to run a Sonatype Scan (can be 'true' or 'false')        
 -h        | -help              | No        | Print help dialog                                                
 -v        | -version           | No        | Print jar version                                                

*One of either apiCredentials or userCredentials is required.

### Legacy
A legacy tag (-l) is also available if you want to access the old format. Simply append the legacy tag at the beginning of your list of arguments.

The old format is similar to below:

```
java -jar FodUpload.jar [-version] ['api key' 'api secret' | 'username' 'password'] 'bsiUrl' 
    'payloadLocation' ['proxyUrl' ['proxyUsername' 'proxyPassword' 'ntDomain' 'ntWorkstation']] 
    [-pollingInterval <minutes>]
```
*Note: if you use an api key, you must append 'key-' to the beginning.*
