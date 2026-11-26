$ErrorActionPreference = 'Stop'
$diretorio = Join-Path $PSScriptRoot '..\keys'
$privada = Join-Path $diretorio 'privateKey.pem'
$publica = Join-Path $diretorio 'publicKey.pem'
if ((Test-Path $privada) -or (Test-Path $publica)) {
    throw 'Uma chave já existe em keys/. A chave deve ser preservada ou copiada antes da geração de outra.'
}
New-Item -ItemType Directory -Path $diretorio -Force | Out-Null
$rsa = [System.Security.Cryptography.RSA]::Create(2048)
try {
    [System.IO.File]::WriteAllText($privada, $rsa.ExportPkcs8PrivateKeyPem())
    [System.IO.File]::WriteAllText($publica, $rsa.ExportSubjectPublicKeyInfoPem())
}
finally { $rsa.Dispose() }
Write-Output 'Chaves locais criadas em keys/.'
