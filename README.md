# SMS_ONBOARD

## AWS λ service for on-boarding new members via sms. Integrates with PowerBase DB.  

### Deploy

```shell
$ serverless deploy
```

### Redeploy Function

```
$ serverless deploy function -f sms
```

### Invoke

```shell
$ curl -X POST <url> -H 'Content-Type: application/json' -d '{"body": "Hi"}'
```

### Logs

```
$ serverless logs -f sms -t
```

### Remove all functions from aws account

Remove Cloud Formation project then run:

```
$ serverless remove
```

### Twilio SMS Config

1. Update POST endpoint on [twilio's sms app config](https://www.twilio.com/console/sms/services/MG7c87df1f861b9b9b5fdbb7404048376e)


### Test

```sh
$ lein doo node sms-onboard-test
```

### EMAIL FN Environment Variables

- DOMAIN=
- MAILGUN_KEY=
- EMAIL_FROM=
- EMAIL_TO=
- EXPORT_FREQUENCY=
- ORG_NAME=
- FIREBASE_ENDPOINT=

### SMS FN Environment Variables

- ORG_NAME=
- FIREBASE_ENDPOINT=

### AWS Environment Variables For Serverless Deployment

- AWS_ACCESS_KEY_ID=
- AWS_SECRET_ACCESS_KEY=