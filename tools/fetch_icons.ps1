param(
    [string]$Out = "D:\Dev\ailua\wowP6\tools\out",
    [string]$Icons = "D:\Dev\ailua\wowP6\app\src\main\assets\icons"
)
$ErrorActionPreference = "Stop"
New-Item -ItemType Directory -Force -Path $Icons | Out-Null

$ref = Get-Content -Raw -LiteralPath (Join-Path $Out "ref_ids.json") | ConvertFrom-Json
$total = $ref.Count
$iconMap = @{}
$nameMap = @{}
$i = 0

foreach ($id in $ref) {
    $i++
    $url = "https://nether.wowhead.com/tooltip/item/$id"
    try {
        $resp = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 20
        $obj = $resp.Content | ConvertFrom-Json
        if ($obj.icon) { $iconMap["$id"] = $obj.icon }
        if ($obj.name) { $nameMap["$id"] = $obj.name }
    } catch {
        # 404 or region error -> ignore
    }
    if ($i % 50 -eq 0) { Write-Output "queried $i / $total  icons=$($iconMap.Count) names=$($nameMap.Count)" }
}

Write-Output "TOTAL items=$total icons=$($iconMap.Count) names=$($nameMap.Count)"

$iconMap | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $Out "icon_map.json") -Encoding UTF8
$nameMap | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $Out "name_map.json") -Encoding UTF8

$uniq = $iconMap.Values | Sort-Object -Unique
Write-Output "unique icons = $($uniq.Count)"

$ok = 0; $fail = 0; $n = 0
foreach ($ic in $uniq) {
    $n++
    $dst = Join-Path $Icons "$ic.jpg"
    if ((Test-Path -LiteralPath $dst) -and ((Get-Item -LiteralPath $dst).Length -gt 0)) { $ok++; continue }
    $got = $false
    foreach ($base in @("large", "medium")) {
        $u = "https://wow.zamimg.com/images/wow/icons/$base/$ic.jpg"
        try {
            $r = Invoke-WebRequest -Uri $u -UseBasicParsing -TimeoutSec 20
            if ($r.StatusCode -eq 200 -and $r.Content.Length -gt 200) {
                [System.IO.File]::WriteAllBytes($dst, $r.Content)
                $got = $true; break
            }
        } catch {}
    }
    if ($got) { $ok++ } else { $fail++; Write-Output "icon FAIL $ic" }
    if ($n % 50 -eq 0) { Write-Output "icons $n / $($uniq.Count) ok=$ok fail=$fail" }
}
Write-Output "DONE ok=$ok fail=$fail"
