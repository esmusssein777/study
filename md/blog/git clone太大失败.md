```
git config --global http.postBuffer 524288000
```

或者先把表层的clone，再把原纪录复制下来

```
$ git clone http://github.com/large-repository --depth 1
$ cd large-repository
$ git fetch --unshallow
```