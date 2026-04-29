# Bottlet
Bottlet is a Paper plugin designed to supercede my previous plugin, [XPManager](https://github.com/jwkerr/XPManager), which itself was a drop-in replacement for the plugin BottledExp.

Bottlet is once again a drop-in replacement, it will work seamlessly with any XPManager or BottledExp bottles.

Folia compatibility has been a core concern of developing this plugin. It should work without issues on a Folia server.

## Examples
```
Get 64 normal bottles.
/bottle get 64

Store 192 bottles with 1395 experience.
/bottle store experience 1395 192

Store the maximum experience in a bottle given your level.
/bottle store experience max

Store 50 levels of experience (from zero).
/bottle store levels 50

Store the maximum bottles of 30 levels of experience (from zero) given your level.
/bottle store levels 30 max

Convert all bottles in your inventory to a singular bottle.
/bottle convert

Repair all mendable items in your inventory.
/bottle mend all

ADMIN COMMANDS
Give all online users 2000 experience in a single bottle.
/ba give @a experience 2000

Give a user 64 bottles of 30 levels.
/ba give Username levels 30 64
```
