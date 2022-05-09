**Error**
```
The authenticity of host 'github.com (140.82.113.4)' can't be established.
```

**Fix**
```sh
ssh-keyscan github.com >> ~/.ssh/known_hosts
```

**Example** (eg. using CircleCI workflow)
```yaml
- run:
    name: Add github.com to known hosts
    command: ssh-keyscan github.com >> ~/.ssh/known_hosts
```
