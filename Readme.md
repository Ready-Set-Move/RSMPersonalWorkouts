# Development

## M1

To make gradle sync with cocoapods behave on M1 MacBooks you need to follow these instructions:

### homebrew

Homebrew needs to be installed under `/opt/homebrew`. If that's not the case follow [these instructions](https://docs.brew.sh/Installation#untar-anywhere).

If you have multiple installations of homebrew, make sure to use the /opt/homebrew one by adding the following to your `~/.zshrc`:

```
cd /opt
eval "$(homebrew/bin/brew shellenv)"
```

### Cocoapods

Simply `sudo gem uninstall cocoapods` and then `brew install cocoapods`. Now the project should sync. If not follow the instructions below with a different ruby version.

### Ruby

Then follow [these steps](https://youtrack.jetbrains.com/issue/KT-49418#focus=Comments-27-5429773.0-0)

But use `echo 'export PATH=/opt/homebrew/opt/ruby@2.7/bin:/opt/homebrew/lib/ruby/gems/2.7.0/bin:"$PATH"' >> ~/.zshrc` instead of similar command in the comment.
