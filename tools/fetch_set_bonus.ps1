param(
    [string]$Out = "D:\Dev\ailua\wowP6\tools\out"
)
$ErrorActionPreference = "Continue"
$need = Get-Content -Raw -Encoding UTF8 -LiteralPath (Join-Path $Out "sets_need.json") | ConvertFrom-Json
$res = @()
foreach ($n in $need) {
    $key = $n.key
    $rep = $n.repItem
    $en = $null; $b2 = $null; $b4 = $null
    if ($rep) {
        try {
            $r = Invoke-WebRequest ("https://nether.wowhead.com/wotlk/tooltip/item/{0}" -f $rep) -UseBasicParsing -TimeoutSec 25
            $j = $r.Content | ConvertFrom-Json
            $t = $j.tooltip
            $m = [regex]::Match($t, 'item-set=[^"]*"[^>]*>([^<]+)</a>')
            if ($m.Success) { $en = [System.Net.WebUtility]::HtmlDecode($m.Groups[1].Value) }
            $rx = [regex]::new('\((\d)\) Set : (.*?)</span>', [System.Text.RegularExpressions.RegexOptions]::Singleline)
            foreach ($mm in $rx.Matches($t)) {
                $txt = [System.Net.WebUtility]::HtmlDecode(($mm.Groups[2].Value -replace '<[^>]+>', '')).Trim()
                if ($mm.Groups[1].Value -eq '2') { $b2 = $txt }
                elseif ($mm.Groups[1].Value -eq '4') { $b4 = $txt }
            }
        } catch {}
    }
    $res += [PSCustomObject]@{ key = $key; en = $en; b2 = $b2; b4 = $b4 }
    Write-Output ("{0} | en={1} b2={2}" -f $key, $en, ($null -ne $b2))
}
$res | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $Out "sets_bonus.json") -Encoding UTF8
$got = ($res | Where-Object { $_.b2 -or $_.b4 }).Count
Write-Output ("DONE total={0} withBonus={1}" -f $res.Count, $got)
