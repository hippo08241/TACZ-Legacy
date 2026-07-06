# TACZ-Legacy PR publish script
# Fork source + PR target: hippo08241/TACZ-Legacy
# Your GitHub account (push destination): yangleeho
#
# Run in PowerShell:
#   1) gh auth login
#   2) .\publish-pr.ps1

$ErrorActionPreference = "Stop"
$env:Path = "C:\Program Files\Git\bin;C:\Program Files\GitHub CLI;" + $env:Path

function Invoke-Git {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Args)

    $prev = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = & git @Args 2>&1
        if ($LASTEXITCODE -ne 0) {
            $text = ($output | Out-String).Trim()
            throw "git $($Args -join ' ') failed (exit $LASTEXITCODE)`n$text"
        }
        return $output
    } finally {
        $ErrorActionPreference = $prev
    }
}

function Test-GitHead {
    $prev = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        & git rev-parse --verify HEAD 2>$null | Out-Null
        return ($LASTEXITCODE -eq 0)
    } finally {
        $ErrorActionPreference = $prev
    }
}

function Invoke-Gh {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$Args)

    $prev = $ErrorActionPreference
    $ErrorActionPreference = "Continue"
    try {
        $output = & gh @Args 2>&1
        if ($LASTEXITCODE -ne 0) {
            $text = ($output | Out-String).Trim()
            throw "gh $($Args -join ' ') failed (exit $LASTEXITCODE)`n$text"
        }
        return $output
    } finally {
        $ErrorActionPreference = $prev
    }
}

$RepoRoot = $PSScriptRoot
$Branch = "fix/legacy-parity-0.2.4"
$YourAccount = "yangleeho"
$TargetOwner = "hippo08241"
$RepoName = "TACZ-Legacy"
$BaseBranch = "master"

Set-Location $RepoRoot

Write-Host "Checking GitHub login..."
Invoke-Gh auth status | Out-Null

$ghUser = Invoke-Gh api user | ConvertFrom-Json
$GitUserName = if ($ghUser.name) { $ghUser.name } else { $ghUser.login }
$GitUserEmail = if ($ghUser.email) { $ghUser.email } else { "$($ghUser.id)+$($ghUser.login)@users.noreply.github.com" }
Write-Host "Using git author: $GitUserName <$GitUserEmail> (local to this repo only)"

if (-not (Test-Path ".git")) {
    Invoke-Git init | Out-Null
}

Invoke-Git config user.name $GitUserName | Out-Null
Invoke-Git config user.email $GitUserEmail | Out-Null

$existingRemotes = @(Invoke-Git remote)
foreach ($remoteName in @("origin", "upstream")) {
    if ($existingRemotes -contains $remoteName) {
        Invoke-Git remote remove $remoteName | Out-Null
    }
}
Invoke-Git remote add origin "https://github.com/$YourAccount/$RepoName.git" | Out-Null
Invoke-Git remote add upstream "https://github.com/$TargetOwner/$RepoName.git" | Out-Null

Write-Host "Forking from $TargetOwner/$RepoName to your account ($YourAccount) if needed..."
try {
    Invoke-Gh repo fork "https://github.com/$TargetOwner/$RepoName" --clone=false | Out-Null
} catch {
    Write-Host "Fork step skipped or already exists."
}

Write-Host "Fetching upstream $BaseBranch..."
Invoke-Git fetch upstream $BaseBranch | Out-Null

$upstreamRef = "upstream/$BaseBranch"
Write-Host "Preparing branch $Branch on top of $upstreamRef (keeping your local files)..."

$onOurBranch = $false
if (Test-GitHead) {
    $currentBranch = (Invoke-Git branch --show-current | Out-String).Trim()
    if ($currentBranch -eq $Branch) {
        $onOurBranch = $true
    }
}

if (-not $onOurBranch) {
    Invoke-Git branch -f $Branch $upstreamRef | Out-Null
    Invoke-Git symbolic-ref HEAD "refs/heads/$Branch" | Out-Null
    Invoke-Git reset --mixed $upstreamRef | Out-Null
}

Invoke-Git add src/main gradle.properties build.gradle.kts settings.gradle.kts gradle/wrapper publish-pr.ps1
try {
    Invoke-Git reset HEAD build | Out-Null
} catch {
    # build/ may not be staged
}

$status = (Invoke-Git status --porcelain | Out-String).Trim()
if ([string]::IsNullOrWhiteSpace($status)) {
    Write-Host "No changes to commit."
} else {
    Invoke-Git commit -m "fix: scope, refit UI, audio, and ammo_box parity (0.1.1-0.2.4)" | Out-Null
}

Write-Host "Pushing branch to your fork..."
Invoke-Git push -u origin $Branch --force-with-lease | Out-Null

Write-Host "Creating pull request to $TargetOwner/$RepoName ..."
Invoke-Gh pr create `
    --repo "$TargetOwner/$RepoName" `
    --head "$YourAccount`:$Branch" `
    --base $BaseBranch `
    --title "fix: scope, refit UI, audio, and ammo_box parity for Legacy 1.12.2" `
    --body "## Summary`n- Fix gun pack / animation sound routing`n- Dry fire sound and creative reload`n- ammo_box model, textures, and inventory`n- Scope stencil/reticle rendering (1.20.1 parity)`n- Refit unload button and GUI textures`n`n## Test plan`n- [ ] Gun fire and reload sounds`n- [ ] ammo_box in hand and inventory`n- [ ] Scope ADS zoom and reticle`n- [ ] Refit unload button visible"

Write-Host "Done. PR should appear at: https://github.com/$TargetOwner/$RepoName/pulls"
