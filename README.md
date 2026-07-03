# PbWarps

> This mod was originally created by Patbox. I vibecoded changes for use on the Afterlife creative server.

This is a simple, admin-defined warp mod for Fabric servers, allowing you to define areas players should have quick access to.
Player's can easily use it by running `/warp NAME` to teleport to selected warp or just run `/warp` to open an ui to select
from all available ones.

Warps can have a custom formatted name, icons, positions/rotation and predicate, limiting who can use it.
This mod has support for using polymer resource packs for nicer looking ui.

![](https://i.imgur.com/IEzKZQB.png)

## Admin Usage.

All admin commands are found under `/warps` command.

To create warp, you use `/warps create <id>`. You can also supply multiple additional parameters to configure it.

You can also modify existing warps by using `/warps modify <id> ...` and all of it's subcommands.

The predicates use Predicate API, for which format is described here: https://github.com/Patbox/PredicateAPI/blob/1.21.2/BUILTIN.md
Only difference being, that the predicate type is passed as it's own argument before predicate data.

You can display info about the warp by using `/warps info <id>` or remove the warp with `/warps remove <id>` command.

## Warp Privacy

Warps can be either **public** or **private**. Private warps are hidden from the selection ui and cannot be teleported to by players who don't own them (warp admins can always see and use any warp).

Set a warp's privacy with `/warps modify <id> privacy <public|private>`. The warp's owner, or any warp admin, may change its privacy.

## Ownership

Every warp has an owner. The owner (or a warp admin) is allowed to change the warp's privacy. Legacy warps created before ownership was added default to AllOutJay.

A warp admin can reassign a warp's owner with `/warps modify <id> setowner <player>`.

## Warp Admins

Warp admins are a permission level above op. Warp admins may view, warp to, and modify **any** private warp, even ones they don't own, and are the only ones permitted to use `/warps teleport`, `/tp`, and `/teleport`.

The list of warp admins lives in `config/pbwarp-admins.json` and is edited by hand. AllOutJay is the built-in default warp admin.
