

```bash
mvn release:branch -DbranchName=REL-2.6-SNAPSHOT -DdevelopmentVersion=2.7-SNAPSHOT
git checkout REL-2.6-SNAPSHOT
mvn -Pdeploy release:prepare release:perform -DreleaseVersion=2.6.0 -DdevelopmentVersion=2.6.1-SNAPSHOT
```

