# K8S 搭建全栈

## 搭建 MySQL

运行下面命令创建 secret

```
$ kubectl create secret generic mysql-root-pass --from-literal=password=toor
secret/mysql-root-pass created

$ kubectl create secret generic mysql-user-pass --from-literal=username=ligz --from-literal=password=toor
secret/mysql-user-pass created

$ kubectl create secret generic mysql-db-url --from-literal=database=docker --from-literal=url='jdbc:mysql://docker-awesome-mysql:3306/docker?useSSL=false&serverTimezone=UTC&useLegacyDatetimeCode=false'
secret/mysql-db-url created
```

使用命令启动 mysql

```bash
kubectl apply -f mysql-deployment.yaml
```

```
apiVersion: v1
kind: PersistentVolume            # Create a PersistentVolume
metadata:
  name: mysql-pv
  labels:
    type: local
spec:
  storageClassName: standard      # Storage class. A PV Claim requesting the same storageClass can be bound to this volume.
  capacity:
    storage: 250Mi
  accessModes:
    - ReadWriteOnce
  hostPath:                       # hostPath PersistentVolume is used for development and testing. It uses a file/directory on the Node to emulate network-attached storage
    path: "/mnt/data"
  persistentVolumeReclaimPolicy: Retain  # Retain the PersistentVolume even after PersistentVolumeClaim is deleted. The volume is considered “released”. But it is not yet available for another claim because the previous claimant’s data remains on the volume.
---
apiVersion: v1
kind: PersistentVolumeClaim        # Create a PersistentVolumeClaim to request a PersistentVolume storage
metadata:                          # Claim name and labels
  name: mysql-pv-claim
  labels:
    app: docker-awesome
spec:                              # Access mode and resource limits
  storageClassName: standard       # Request a certain storage class
  accessModes:
    - ReadWriteOnce                # ReadWriteOnce means the volume can be mounted as read-write by a single Node
  resources:
    requests:
      storage: 250Mi
---
apiVersion: v1                    # API version
kind: Service                     # Type of kubernetes resource
metadata:
  name: docker-awesome-mysql         # Name of the resource
  labels:                         # Labels that will be applied to the resource
    app: docker-awesome
spec:
  ports:
    - port: 3306
  selector:                       # Selects any Pod with labels `app=docker-awesome,tier=mysql`
    app: docker-awesome
    tier: mysql
  clusterIP: None
---
apiVersion: apps/v1
kind: Deployment                    # Type of the kubernetes resource
metadata:
  name: docker-awesome-mysql           # Name of the deployment
  labels:                           # Labels applied to this deployment
    app: docker-awesome
spec:
  selector:
    matchLabels:                    # This deployment applies to the Pods matching the specified labels
      app: docker-awesome
      tier: mysql
  strategy:
    type: Recreate
  template:                         # Template for the Pods in this deployment
    metadata:
      labels:                       # Labels to be applied to the Pods in this deployment
        app: docker-awesome
        tier: mysql
    spec:                           # The spec for the containers that will be run inside the Pods in this deployment
      containers:
        - image: mysql:5.7            # The container image
          name: mysql
          env:                        # Environment variables passed to the container
            - name: MYSQL_ROOT_PASSWORD
              valueFrom:                # Read environment variables from kubernetes secrets
                secretKeyRef:
                  name: mysql-root-pass
                  key: password
            - name: MYSQL_DATABASE
              valueFrom:
                secretKeyRef:
                  name: mysql-db-url
                  key: database
            - name: MYSQL_USER
              valueFrom:
                secretKeyRef:
                  name: mysql-user-pass
                  key: username
            - name: MYSQL_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: mysql-user-pass
                  key: password
          ports:
            - containerPort: 3306        # The port that the container exposes
              name: mysql
          volumeMounts:
            - name: mysql-persistent-storage  # This name should match the name specified in `volumes.name`
              mountPath: /var/lib/mysql
      volumes:                       # A PersistentVolume is mounted as a volume to the Pod
        - name: mysql-persistent-storage
          persistentVolumeClaim:
            claimName: mysql-pv-claim


```

```
kubectl get pods
NAME                                   READY   STATUS    RESTARTS   AGE
docker-awesome-mysql-978f668d4-j2c8h   1/1     Running   0          4m50s

kubectl exec -it docker-awesome-mysql-978f668d4-j2c8h -- /bin/bash
mysql -u root -p
```

发现使用 toor 命令可以进入 mysql ，说明我们 secret 和 mysql 都没有问题

## 搭建服务

```
---
apiVersion: apps/v1           # API version
kind: Deployment              # Type of kubernetes resource
metadata:
  name: docker-awesome-server    # Name of the kubernetes resource
  labels:                     # Labels that will be applied to this resource
    app: docker-awesome-server
spec:
  replicas: 1                 # No. of replicas/pods to run in this deployment
  selector:
    matchLabels:              # The deployment applies to any pods mayching the specified labels
      app: docker-awesome-server
  template:                   # Template for creating the pods in this deployment
    metadata:
      labels:                 # Labels that will be applied to each Pod in this deployment
        app: docker-awesome-server
    spec:                     # Spec for the containers that will be run in the Pods
      containers:
        - name: docker-awesome-server
          image: esmusssein777/docker-awesome:4.0
          imagePullPolicy: IfNotPresent
          ports:
            - name: http
              containerPort: 8080 # The port that the container exposes
          resources:
            limits:
              cpu: 0.2
              memory: "500Mi"
          env:                  # Environment variables supplied to the Pod
            - name: SPRING_DATASOURCE_USERNAME # Name of the environment variable
              valueFrom:          # Get the value of environment variable from kubernetes secrets
                secretKeyRef:
                  name: mysql-user-pass
                  key: username
            - name: SPRING_DATASOURCE_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: mysql-user-pass
                  key: password
            - name: SPRING_DATASOURCE_URL
              valueFrom:
                secretKeyRef:
                  name: mysql-db-url
                  key: url
---
apiVersion: v1                # API version
kind: Service                 # Type of the kubernetes resource
metadata:
  name: docker-awesome-server    # Name of the kubernetes resource
  labels:                     # Labels that will be applied to this resource
    app: docker-awesome-server
spec:
  type: NodePort              # The service will be exposed by opening a Port on each node and proxying it.
  selector:
    app: docker-awesome-server   # The service exposes Pods with label `app=docker-awesome-server`
  ports:                      # Forward incoming connections on port 8080 to the target port 8080
    - name: http
      port: 8080
      targetPort: 8080
```

执行 `kubectl apply -f service-deployment.yaml`

使用`kubectl get service`

```
NAME                  TYPE       CLUSTER-IP    EXTERNAL-IP   PORT(S)             AGE
docker-awesome-server NodePort   10.106.23.169  <none>       8080:32701/TCP     4h54m
```

访问`http://172.16.182.130:32701/swagger-ui.html#/` 可以看到页面



