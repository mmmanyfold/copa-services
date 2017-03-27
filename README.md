# copa-λ-services

### Deploy

```shell
$ serverless deploy
```

### Redeploy Function

```
$ serverless deploy function -f echo
```

### Invoke

```shell
$ curl -X POST <url> -H 'Content-Type: application/json' -d '{"body": "Hi"}'
```

### Logs

```
serverless logs -f echo -t
```

### Remove all functions from aws account

```
serverless remove
```
