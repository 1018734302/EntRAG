$ErrorActionPreference = 'Stop'
$dir = 'c:\pycharmwork\EntRAG\resume-docx'

# pick the real document (skip Word ~$ lock files); keep this script ASCII-only
$docx = Get-ChildItem -Path $dir -Filter '*.docx' | Where-Object { $_.Name -notlike '~$*' } | Select-Object -First 1
if (-not $docx) { Write-Host 'NO DOCX FOUND'; exit 1 }
$docxPath = $docx.FullName
Write-Host ('SOURCE: ' + $docx.Name)

$tmp = Join-Path $env:TEMP ('docx2md_' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $tmp | Out-Null
$zipCopy = Join-Path $tmp 'doc.zip'
Copy-Item $docxPath $zipCopy
Expand-Archive -Path $zipCopy -DestinationPath (Join-Path $tmp 'x') -Force
$docXml = Join-Path $tmp 'x\word\document.xml'

[xml]$x = Get-Content -Path $docXml -Encoding UTF8 -Raw
$nsm = New-Object System.Xml.XmlNamespaceManager($x.NameTable)
$W = 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'
$nsm.AddNamespace('w', $W)
$body = $x.SelectSingleNode('/w:document/w:body', $nsm)

$md = New-Object System.Collections.Generic.List[string]
$titleDone = $false
$prevKind = ''

foreach ($node in $body.ChildNodes) {
  if ($node.LocalName -eq 'p') {
    $texts = @($node.SelectNodes('.//w:t', $nsm) | ForEach-Object { $_.InnerText })
    $text = ($texts -join '')
    if ([string]::IsNullOrWhiteSpace($text)) { continue }
    $styleNode = $node.SelectSingleNode('w:pPr/w:pStyle', $nsm)
    $style = ''
    if ($styleNode) { $style = $styleNode.GetAttribute('val', $W) }
    $isList = $null -ne $node.SelectSingleNode('w:pPr/w:numPr', $nsm)
    $isCenter = $null -ne $node.SelectSingleNode("w:pPr/w:jc[@w:val='center']", $nsm)

    $line = ''
    $kind = 'p'
    if ($isCenter) {
      if (-not $titleDone) { $line = '# ' + $text; $titleDone = $true; $kind = 'title' }
      else { $line = '*' + $text + '*'; $kind = 'sub' }
    }
    elseif ($style -eq 'Heading1') { $line = '## ' + $text; $kind = 'h1' }
    elseif ($style -eq 'Heading2') { $line = '### ' + $text; $kind = 'h2' }
    elseif ($isList) { $line = '- ' + $text; $kind = 'list' }
    else { $line = $text; $kind = 'p' }

    if ($md.Count -gt 0) {
      if (-not ($kind -eq 'list' -and $prevKind -eq 'list')) { $md.Add('') }
    }
    $md.Add($line)
    $prevKind = $kind
  }
  elseif ($node.LocalName -eq 'tbl') {
    if ($md.Count -gt 0) { $md.Add('') }
    $r = 0
    foreach ($row in $node.SelectNodes('w:tr', $nsm)) {
      $cells = @($row.SelectNodes('w:tc', $nsm))
      $vals = @()
      foreach ($c in $cells) {
        $vals += (@($c.SelectNodes('.//w:t', $nsm) | ForEach-Object { $_.InnerText }) -join '')
      }
      $md.Add('| ' + ($vals -join ' | ') + ' |')
      if ($r -eq 0) {
        $sep = @(); foreach ($c in $cells) { $sep += '---' }
        $md.Add('| ' + ($sep -join ' | ') + ' |')
      }
      $r++
    }
    $prevKind = 'tbl'
  }
}

$outPath = [System.IO.Path]::ChangeExtension($docxPath, '.md')
$utf8 = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($outPath, ($md -join "`r`n"), $utf8)
Write-Host ('WROTE: ' + [System.IO.Path]::GetFileName($outPath))
Write-Host ('LINES: ' + $md.Count)
Remove-Item -Recurse -Force $tmp -ErrorAction SilentlyContinue
