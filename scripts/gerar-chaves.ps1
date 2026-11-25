$ErrorActionPreference = 'Stop'
$diretorio = Join-Path $PSScriptRoot '..\keys'
$privada = Join-Path $diretorio 'privateKey.pem'
$publica = Join-Path $diretorio 'publicKey.pem'
if ((Test-Path $privada) -or (Test-Path $publica)) {
    throw 'Uma chave já existe em keys/. Preserve-a ou faça backup antes de gerar outras.'
}
New-Item -ItemType Directory -Path $diretorio -Force | Out-Null
$rsa = [System.Security.Cryptography.RSA]::Create(2048)
try {
    [System.IO.File]::WriteAllText($privada, $rsa.ExportPkcs8PrivateKeyPem())
    [System.IO.File]::WriteAllText($publica, $rsa.ExportSubjectPublicKeyInfoPem())
}
finally { $rsa.Dispose() }
Write-Output 'Chaves locais criadas em keys/.'
