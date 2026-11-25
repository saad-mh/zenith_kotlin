# Creates the v2 milestone, labels, issues and epic from docs/v2/issues.md.
# Prereq: `gh auth login`. Run from anywhere:  powershell -File scripts/create-v2-issues.ps1 [-DryRun]
param([switch]$DryRun)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$file = Join-Path $root 'docs/v2/issues.md'
$milestone = 'v2.0 - Circles revamp'
$designUrl = 'https://claude.ai/code/artifact/c611c1a9-7edf-48e1-86be-875a57f1141b'

# ---- parse -------------------------------------------------------------
$text = (Get-Content $file -Raw -Encoding UTF8) -replace '(?s)^<!--.*?-->', ''
$chunks = [regex]::Split($text, '(?m)^## (?=\[)') | Where-Object { $_.Trim() }
$issues = @()
foreach ($c in $chunks) {
  $lines = $c -split "`r?`n"
  if ($lines[0] -notmatch '^\[(\w+)\]\s+(.+)$') { throw "Bad heading: $($lines[0])" }
  $id = $Matches[1]; $title = $Matches[2].Trim()
  $labels = @(($lines[1] -replace '^Labels:\s*', '') -split ',' | ForEach-Object { $_.Trim() } | Where-Object { $_ })
  $body = (($lines | Select-Object -Skip 2) -join "`n").Trim()
  $issues += [pscustomobject]@{ Id = $id; Title = $title; Labels = $labels; Body = $body; Number = $null }
}
Write-Host "Parsed $($issues.Count) issues"

if ($DryRun) { $issues | ForEach-Object { "{0,-4} {1}  [{2}]" -f $_.Id, $_.Title, ($_.Labels -join ', ') }; return }

gh auth status | Out-Null
if ($LASTEXITCODE -ne 0) { throw 'Run `gh auth login` first.' }

# ---- labels ------------------------------------------------------------
$colors = @{ 'v2' = '5a5fb0'; 'epic' = '241f2b'; 'needs-decision' = 'c1493f'; 'safety' = 'c1493f'; 'tech-debt' = '6d6877'; 'a11y' = '1f7a4f' }
$allLabels = ($issues.Labels | Sort-Object -Unique) + 'epic'
foreach ($l in ($allLabels | Sort-Object -Unique)) {
  $col = if ($colors.ContainsKey($l)) { $colors[$l] } elseif ($l -like 'phase:*') { '9ea4de' } else { 'b79b5e' }
  gh label create $l --color $col --force | Out-Null
}

# ---- milestone ---------------------------------------------------------
$existing = gh api "repos/{owner}/{repo}/milestones?state=all" --jq ".[] | select(.title==`"$milestone`") | .number"
if (-not $existing) { gh api "repos/{owner}/{repo}/milestones" -f title="$milestone" -f description='Visual and structural revamp (Circles direction). See docs/v2/PLAN.md.' | Out-Null }

# ---- pass 1: create ----------------------------------------------------
$tmp = New-TemporaryFile
foreach ($i in $issues) {
  $footer = "`n`n---`nPart of the v2 revamp. Plan: ``docs/v2/PLAN.md`` | Design canvas: $designUrl"
  Set-Content $tmp ($i.Body + $footer) -Encoding UTF8
  $url = gh issue create --title $i.Title --body-file $tmp --label ($i.Labels -join ',') --milestone $milestone
  $i.Number = [int]($url.Trim() -replace '.*/', '')
  Write-Host ("#{0,-4} {1}" -f $i.Number, $i.Title)
}

# ---- pass 2: resolve [[ID]] references ----------------------------------
$map = @{}; $issues | ForEach-Object { $map[$_.Id] = $_.Number }
foreach ($i in $issues) {
  if ($i.Body -notmatch '\[\[\w+\]\]') { continue }
  $resolved = [regex]::Replace($i.Body, '\[\[(\w+)\]\]', { param($m) "#$($map[$m.Groups[1].Value])" })
  $footer = "`n`n---`nPart of the v2 revamp. Plan: ``docs/v2/PLAN.md`` | Design canvas: $designUrl"
  Set-Content $tmp ($resolved + $footer) -Encoding UTF8
  gh issue edit $i.Number --body-file $tmp | Out-Null
}

# ---- epic ---------------------------------------------------------------
$phases = [ordered]@{ 'phase:0-foundation' = 'Phase 0 - Foundation'; 'phase:1-data' = 'Phase 1 - Data'; 'phase:2-screens' = 'Phase 2 - Screens'; 'phase:3-quality' = 'Phase 3 - Quality and release' }
$epic = "# Zenith v2 - Circles revamp`n`nTracking issue. Direction, tokens and decisions: ``docs/v2/PLAN.md``. Design canvas: $designUrl`n`n"
foreach ($p in $phases.Keys) {
  $epic += "## $($phases[$p])`n"
  foreach ($i in ($issues | Where-Object { $_.Labels -contains $p })) { $epic += "- [ ] #$($i.Number) $($i.Title)`n" }
  $epic += "`n"
}
$epic += "## Open decisions`nSee section 5 of the plan; issues needing an answer carry the ``needs-decision`` label.`n"
Set-Content $tmp $epic -Encoding UTF8
gh issue create --title 'Epic: Zenith v2 - Circles revamp' --body-file $tmp --label 'epic,v2' --milestone $milestone
Remove-Item $tmp
