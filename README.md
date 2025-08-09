### YTComments 0.0.5
Utility for parsing YouTube html and downloading comments or transcript from videos.

#### Usage

hard code in the Main:  (sorry for that)

```
Downloader d = new Downloader(URL);
d.download();
```
or, to download transcript:

```
getTranscript(d);
```

see result at console. Or you can write something to file:

```
writeToFile(string, path, filename);
```
