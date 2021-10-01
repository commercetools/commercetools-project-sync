
Running a **Full sync** using `-f` or `--full` option will not create any `customObjects`.

#### Running Multiple Syncers

The application can sync multiple resources. For example, to run `type` and `productType` sync together, 
the  `-s` option with `types productTypes` as below:
```bash
-s types productTypes
```

#### Running ProductSync with custom product query parameters

You might pass your customized product fetch limit, and a product projection predicate to filter product resources to sync in the JSON format.

For instance:

```bash
-s products -productQueryParameters "{\"limit\": 100, \"where\": \"published=true AND masterVariant(attributes(name=\\\"attribute-name\\\" and value=\\\"attribute-value\\\"))\"}"
```

Predicates provide a way for complex filter expressions when querying resources. Refer commercetools docs for more details.

Note: The value of the productQueryParameters argument should be in JSON format and as shown in the above example, please use escape character \ for the nested double quote values.
Example: 
```bash
-s products -productQueryParameters "{\"limit\": 100, \"where\": \"published=true AND masterVariant(key= \\\"variantKey\\\")\"}"
```

#### Running the Docker Image

##### Download

```bash
docker pull commercetools/commercetools-project-sync:4.0.10
```
##### Run

```bash
docker run \
-e SOURCE_PROJECT_KEY=xxxx \
-e SOURCE_CLIENT_ID=xxxx \
-e SOURCE_CLIENT_SECRET=xxxx \
-e TARGET_PROJECT_KEY=xxxx \
-e TARGET_CLIENT_ID=xxxx \
-e TARGET_CLIENT_SECRET=xxxx \
commercetools/commercetools-project-sync:4.0.10 -s all
```