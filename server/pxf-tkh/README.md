# PXF Clickhouse plugin
The PXF Clickhouse plugin implements Clickhouse HTTP interface and allows to INSERT data via PXF into Clickhouse.


## Syntax
```
CREATE [ READABLE | WRITABLE ] EXTERNAL TABLE <table_name> (
    { <column_name> <data_type> [, ...] | LIKE <other_table> }
)
LOCATION (
    'pxf://<full_external_table_name>?<pxf_parameters>[&SERVER=<server_name>]<settings>'
)
FORMAT 'CUSTOM' (FORMATTER='pxfwritable_export')
```

The **`<pxf_parameters>`** are:
```
{
PROFILE=CLICKHOUSE
|
ACCESSOR=org.greenplum.pxf.plugins.jdbc.TkhAccessor
&RESOLVER=org.greenplum.pxf.plugins.jdbc.TkhResolver
}
```

**`<server_name>`** and **`<settings>`** are described in [Plugin settings section](#plugin-settings).


## Plugin settings
The plugin has several settings. They can be set in two sites:
* `LOCATION` clause of an external table DDL. Every setting must have format `&<name>=<value>`. Hereinafter setting set in `LOCATION` clause is referred to as "option".
* Configuration file located at `$PXF_CONF/servers/<server_name>/tkh-site.xml` (on every PXF segment), where `<server_name>` is an arbitrary name (the file is intended to include options specific for each external database server). Hereinafter setting set in configuration file is referred to as "configuration parameter".

If `SERVER` option is not set in external table DDL, PXF will assume it equal to `default` and load configuration files from `$PXF_CONF/servers/default/tkh-site.xml`. A warning is added to PXF log file if `SERVER` is set to incorrect value (PXF is unable to read the requested configuration file).

If a setting can be set by both option and configuration parameter, option value overrides configuration parameter value.

Note that if setting is provided, its value is checked for correctness.


### List of plugin settings
#### Distribution type
Clickhouse connection distribution type. See explanation below.

* **Option**: `DISTRIBUTION`
* **Configuration parameter**: `clickhouse.distribution`
* **Value**: One of the following:
    * `LIST`
* **Default value**: `LIST`


#### URL
Clickhouse URL. The actual meaning depends on [Distribution type](#distribution-type) setting value.

* **Option**: `URL`
* **Configuration parameter**: `clickhouse.url`
* **Value**: String


#### Batch size
Batch size to use when sending data to Clickhouse.

Note that Clickhouse works significantly faster if given a small number of huge batches, compared to a large number of smaller batches. However, the processing time increases as the volume of data increases. The most efficient batch size depends on the data structure.

* **Option**: `BATCH`
* **Configuration parameter**: `clickhouse.batch`
* **Value**: Integer >= 0
* **Default value**: 49152


#### Timeout
HTTP timeout in milliseconds.

* **Option**: `TIMEOUT`
* **Configuration parameter**: `clickhouse.timeout`
* **Value**: String
* **Default value**: 10000


## Distribution
Clickhouse plugin is intended to support multiple types of data distribution across multiple servers.

Every type of distribution changes the behaviour of Clickhouse plugin when it selects the Clickhouse machine to send data to. This allows to tweak load and preserve the consistency guarantees defined by user.

Every distribution type may require different settings of the plugin to be set.

The description of supported distribution types is given below.


### List of hosts (`LIST` [distribution type](#distribution-type))
Choose a host randomly from a given list.

#### Settings
* **[`URL`](#URL)** (required). A comma-separated list of URLs to use. Whitespace and comma characters are removed; encode them properly if the URL contains such characters.


### TBA
