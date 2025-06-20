
$files = Get-Content "diff_list.txt" | Select-Object -Skip 20

foreach ($file in $files) {
  
    $cleanPath = $file -replace "^[A-Z]\s+", ""

    Write-Host "`n>>> open $cleanPath"
    git difftool manual_fix broken_code -- "$cleanPath"

    $answer = Read-Host "press enter or q for exit"
    if ($answer -eq 'q') {
        break
    }
}
