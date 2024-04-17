# Features

Following features are required to support JSON schema:

## Union rules

Main node of a JSON schema is a union of basic types (e.g. `ArraySchema|ObjectSchema|...`).
We need to be able to define a set of rules to distinguish which union member should be used.

## Type aliases

To avoid repeating the full (union) type definition, it should be possible to define a type alias,
and reference it where appropriate.

## Union roots

At the moment, only a specific entity can be marked as model root:

```yaml
entities:
  - name: Document
    root: true
    traits:
      - Extensible
    properties:      
      - name: info
        type: Info
      # etc.
    propertyOrder:
      - $this
      - $Extensible
```

We need to define a general `Schema` union as a root of the model.

## Non-object model nodes

Support model nodes that represent a primitive value, i.e. not a JSON object (or array?)

