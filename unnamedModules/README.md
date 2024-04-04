## Unnamed modules

Some libraries are not modular and do not provide the Automatic-Module-Name attribute in the manifest, making them incompatible with modular projects.

To address this issue, project includes modules for each unnamed library. Each module includes a Gradle task for repackaging the original JAR file, adding the
appropriate Automatic-Module-Name attribute automatically. This ensures seamless integration with modular projects without requiring any manual intervention from
users.
