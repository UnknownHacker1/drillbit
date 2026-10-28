<#
.SYNOPSIS
  Publishes Drillbit in one go. Safe to re-run (each step is skipped when already done).
   1. Creates the public repo UnknownHacker1/drillbit (if missing) and pushes main.
   2. Turns on GitHub Pages with "GitHub Actions" as the source.
   3. Runs the "Deploy web app" workflow and waits for it (about 10 min).
   4. Builds the debug APK and attaches it to release v1.0.0 (skip with -NoApk).
   5. Checks https://unknownhacker1.github.io/drillbit/ answers and prints it.
.EXAMPLE
  .\publish.ps1
.EXAMPLE
  .\publish.ps1 -DryRun      # read-only checks, prints what it would do
.EXAMPLE
  .\publish.ps1 -Redeploy    # run the Pages workflow again even if the current commit is deployed
#>
param(
  [string]$Owner = 'UnknownHacker1',
  [string]$Name = 'drillbit',
  [switch]$NoApk,
  [switch]$Redeploy,
  [switch]$DryRun
)
$ErrorActionPreference = 'Continue'
Set-Location -LiteralPath $PSScriptRoot
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
function Fail([string]$msg) { Write-Host "ERROR: $msg" -ForegroundColor Red; exit 1 }
function Step([string]$msg) { Write-Host ""; Write-Host "== $msg" -ForegroundColor Cyan }
function Dry([string]$what) { Write-Host "[dry-run] would run: $what" -ForegroundColor Yellow }
$R = "$Owner/$Name"
$repoUrl = "https://github.com/$R"
$pagesUrl = "https://$($Owner.ToLower()).github.io/$Name/"
$jbr = 'C:\Program Files\Android\Android Studio\jbr'

Step '1/5 Repo and push'
if (-not (Get-Command gh -ErrorAction SilentlyContinue)) { Fail 'GitHub CLI (gh) is not installed: https://cli.github.com' }
gh auth status *> $null
if ($LASTEXITCODE -ne 0) { Fail 'gh is not logged in. Run: gh auth login' }
$branch = (git rev-parse --abbrev-ref HEAD).Trim()
if ($branch -ne 'main') { Fail "You are on branch '$branch'. Run: git checkout main" }
$dirty = git status --porcelain --untracked-files=no
if ($dirty) { Write-Host "Note: uncommitted changes are NOT pushed:`n$($dirty -join "`n")" -ForegroundColor Yellow }
$head = (git rev-parse HEAD).Trim()

$repoExists = $false
gh api "repos/$R" --silent 2>$null
if ($LASTEXITCODE -eq 0) { $repoExists = $true }
$origin = git remote get-url origin 2>$null
if ($LASTEXITCODE -eq 0 -and $origin) {
  Write-Host "Remote 'origin': $origin"
} elseif ($repoExists) {
  if ($DryRun) { Dry "git remote add origin $repoUrl.git" } else { git remote add origin "$repoUrl.git" }
} elseif ($DryRun) {
  Dry "gh repo create $R --public --source . --remote origin"
} else {
  gh repo create $R --public --source . --remote origin --description 'Drillbit: collision-free marching band formations, Kotlin Multiplatform (Android, desktop, web)'
  if ($LASTEXITCODE -ne 0) { Fail 'gh repo create failed (see above).' }
  $repoExists = $true
}
if ($DryRun) { Dry 'git push -u origin main' } else {
  $out = git push -u origin main 2>&1 | ForEach-Object { "$_" }
  $out | ForEach-Object { Write-Host $_ }
  if ($LASTEXITCODE -ne 0) {
    if (($out -join ' ') -match 'workflow') {
      Fail "GitHub refused the workflow file: your login lacks the 'workflow' scope. Run these two, then this script again:`n  gh auth refresh -h github.com -s workflow`n  gh auth setup-git"
    }
    Fail 'git push failed (see above).'
  }
}

Step '2/5 GitHub Pages (source: GitHub Actions)'
if (-not $repoExists -and $DryRun) { Dry "gh api -X POST repos/$R/pages -f build_type=workflow" } else {
  $bt = gh api "repos/$R/pages" --jq .build_type 2>$null
  if ($LASTEXITCODE -eq 0 -and $bt -eq 'workflow') { Write-Host 'Already on, source GitHub Actions.' }
  elseif ($DryRun) { Dry "gh api -X POST repos/$R/pages -f build_type=workflow (or PUT if Pages exists)" }
  else {
    if ($LASTEXITCODE -eq 0) { $res = gh api -X PUT "repos/$R/pages" -f build_type=workflow 2>&1 }   # exists, wrong source
    else { $res = gh api -X POST "repos/$R/pages" -f build_type=workflow 2>&1 }
    if ($LASTEXITCODE -ne 0 -and ("$res" -notmatch 'already exists')) { Fail "Could not turn on Pages: $res" }
    Write-Host 'Pages on, source GitHub Actions.'
  }
}

Step '3/5 Deploy web app workflow'
$deployed = $false
if ($repoExists) {
  $last = gh run list -R $R --workflow pages.yml --limit 20 --json databaseId,headSha,status,conclusion 2>$null | ConvertFrom-Json
  if ($last | Where-Object { $_.headSha -eq $head -and $_.conclusion -eq 'success' }) { $deployed = $true }
}
if ($deployed -and -not $Redeploy) { Write-Host "Commit $($head.Substring(0,7)) is already deployed (use -Redeploy to run it again)." }
elseif ($DryRun) { Dry "gh workflow run pages.yml -R $R --ref main; gh run watch <id> -R $R --exit-status" }
else {
  $before = @(gh run list -R $R --workflow pages.yml --limit 50 --json databaseId 2>$null | ConvertFrom-Json | ForEach-Object { $_.databaseId })
  $ok = $false
  for ($i = 0; $i -lt 6 -and -not $ok; $i++) {   # a brand-new repo can take a few seconds to register the workflow
    gh workflow run pages.yml -R $R --ref main 2>$null
    if ($LASTEXITCODE -eq 0) { $ok = $true } else { Start-Sleep -Seconds 5 }
  }
  if (-not $ok) { Fail "Could not start the workflow. Open $repoUrl/actions, click 'Deploy web app', then 'Run workflow'." }
  $run = $null
  for ($i = 0; $i -lt 20 -and -not $run; $i++) {
    Start-Sleep -Seconds 3
    $run = gh run list -R $R --workflow pages.yml --limit 5 --json databaseId 2>$null | ConvertFrom-Json |
      Where-Object { $before -notcontains $_.databaseId } | Select-Object -First 1
  }
  if (-not $run) { Fail "The run did not show up. Check $repoUrl/actions" }
  Write-Host "Watching run $($run.databaseId) (about 10 min; Ctrl+C stops watching, not the run)..."
  gh run watch $run.databaseId -R $R --exit-status --interval 15
  if ($LASTEXITCODE -ne 0) { Fail "The workflow failed. Logs: $repoUrl/actions/runs/$($run.databaseId)" }
}

Step '4/5 APK release v1.0.0'
if ($NoApk) { Write-Host 'Skipped (-NoApk).' } else {
  $relExists = $false
  if ($repoExists) { gh release view v1.0.0 -R $R --json tagName *> $null; if ($LASTEXITCODE -eq 0) { $relExists = $true } }
  if ($relExists) { Write-Host "Release v1.0.0 already exists: $repoUrl/releases/tag/v1.0.0" }
  else {
    if (-not (Test-Path -LiteralPath $jbr)) { Fail "Java not found at $jbr (Android Studio's JBR). Install Android Studio or rerun with -NoApk." }
    $env:JAVA_HOME = $jbr
    $apk = Join-Path $PSScriptRoot 'androidApp\build\outputs\apk\debug\androidApp-debug.apk'
    Write-Host 'Building the debug APK...'
    & .\gradlew.bat :androidApp:assembleDebug --quiet
    if ($LASTEXITCODE -ne 0 -or -not (Test-Path -LiteralPath $apk)) { Fail 'APK build failed (see above). The web app is already live; rerun later or use -NoApk.' }
    Write-Host ("APK: {0:N1} MB" -f ((Get-Item -LiteralPath $apk).Length / 1MB))
    if ($DryRun) { Dry "gh release create v1.0.0 <apk> -R $R --target main --title 'Drillbit 1.0.0'" }
    else {
      gh release create v1.0.0 "$apk" -R $R --target main --title 'Drillbit 1.0.0' --notes 'Debug APK for Android 8.0+. The web version needs no install: https://unknownhacker1.github.io/drillbit/'
      if ($LASTEXITCODE -ne 0) { Fail 'gh release create failed (see above).' }
    }
  }
}

Step '5/5 Check the web app'
if ($DryRun) { Dry "GET $pagesUrl" } else {
  $up = $false
  for ($i = 0; $i -lt 12 -and -not $up; $i++) {
    try { $r = Invoke-WebRequest -Uri $pagesUrl -UseBasicParsing -TimeoutSec 15; if ($r.StatusCode -eq 200) { $up = $true } } catch { Start-Sleep -Seconds 10 }
  }
  if (-not $up) { Write-Host "Not answering yet (Pages can take a few minutes the first time). Try $pagesUrl again soon." -ForegroundColor Yellow }
}

Write-Host ''
Write-Host "Repo:    $repoUrl" -ForegroundColor Green
Write-Host "Web app: $pagesUrl" -ForegroundColor Green
if (-not $NoApk) { Write-Host "APK:     $repoUrl/releases/tag/v1.0.0" -ForegroundColor Green }
if ($DryRun) { Write-Host ''; Write-Host 'DRY RUN: nothing was created or pushed (the APK may have been built locally).' -ForegroundColor Yellow }
