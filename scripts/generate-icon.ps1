$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $PSScriptRoot
$assets = Join-Path $root "assets"
$pngPath = Join-Path $assets "tinywin-nfs-server.png"
$icoPath = Join-Path $assets "tinywin-nfs-server.ico"

New-Item -ItemType Directory -Path $assets -Force | Out-Null

Add-Type -AssemblyName System.Drawing

function New-RoundedRectanglePath {
	param(
		[float]$X,
		[float]$Y,
		[float]$Width,
		[float]$Height,
		[float]$Radius
	)

	$path = New-Object System.Drawing.Drawing2D.GraphicsPath
	$diameter = $Radius * 2
	$path.AddArc($X, $Y, $diameter, $diameter, 180, 90)
	$path.AddArc($X + $Width - $diameter, $Y, $diameter, $diameter, 270, 90)
	$path.AddArc($X + $Width - $diameter, $Y + $Height - $diameter, $diameter, $diameter, 0, 90)
	$path.AddArc($X, $Y + $Height - $diameter, $diameter, $diameter, 90, 90)
	$path.CloseFigure()
	return $path
}

function Write-IconFromPngBytes {
	param(
		[byte[]]$PngBytes,
		[string]$Path
	)

	$stream = [System.IO.File]::Create($Path)
	try {
		$writer = New-Object System.IO.BinaryWriter($stream)
		$writer.Write([UInt16]0)
		$writer.Write([UInt16]1)
		$writer.Write([UInt16]1)
		$writer.Write([byte]0)
		$writer.Write([byte]0)
		$writer.Write([byte]0)
		$writer.Write([byte]0)
		$writer.Write([UInt16]1)
		$writer.Write([UInt16]32)
		$writer.Write([UInt32]$PngBytes.Length)
		$writer.Write([UInt32]22)
		$writer.Write($PngBytes)
		$writer.Flush()
	} finally {
		$stream.Dispose()
	}
}

$bitmap = New-Object System.Drawing.Bitmap 256, 256, ([System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
$graphics = [System.Drawing.Graphics]::FromImage($bitmap)
$graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
$graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit

try {
	$graphics.Clear([System.Drawing.Color]::Transparent)

	$background = New-RoundedRectanglePath 18 18 220 220 42
	$backgroundBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
			[System.Drawing.RectangleF]::new(18, 18, 220, 220),
			[System.Drawing.Color]::FromArgb(255, 16, 39, 58),
			[System.Drawing.Color]::FromArgb(255, 22, 102, 111),
			[System.Drawing.Drawing2D.LinearGradientMode]::ForwardDiagonal)
	$graphics.FillPath($backgroundBrush, $background)

	$edgePen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 134, 231, 218), 5)
	$graphics.DrawPath($edgePen, $background)

	$folderPath = New-Object System.Drawing.Drawing2D.GraphicsPath
	$folderPath.AddRectangle([System.Drawing.RectangleF]::new(54, 94, 148, 84))
	$folderPath.AddRectangle([System.Drawing.RectangleF]::new(54, 76, 66, 30))
	$folderBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush(
			[System.Drawing.RectangleF]::new(54, 76, 148, 102),
			[System.Drawing.Color]::FromArgb(255, 255, 193, 67),
			[System.Drawing.Color]::FromArgb(255, 239, 139, 42),
			[System.Drawing.Drawing2D.LinearGradientMode]::Vertical)
	$graphics.FillPath($folderBrush, $folderPath)

	$folderPen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 255, 237, 182), 4)
	$graphics.DrawPath($folderPen, $folderPath)

	$linePen = New-Object System.Drawing.Pen([System.Drawing.Color]::FromArgb(255, 134, 231, 218), 8)
	$linePen.StartCap = [System.Drawing.Drawing2D.LineCap]::Round
	$linePen.EndCap = [System.Drawing.Drawing2D.LineCap]::Round
	$graphics.DrawLine($linePen, 78, 174, 78, 205)
	$graphics.DrawLine($linePen, 128, 174, 128, 210)
	$graphics.DrawLine($linePen, 178, 174, 178, 205)
	$graphics.DrawLine($linePen, 78, 205, 178, 205)

	$nodeBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 233, 251, 248))
	$graphics.FillEllipse($nodeBrush, 66, 193, 24, 24)
	$graphics.FillEllipse($nodeBrush, 116, 198, 24, 24)
	$graphics.FillEllipse($nodeBrush, 166, 193, 24, 24)

	$font = New-Object System.Drawing.Font("Segoe UI", 39, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
	$textBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(255, 247, 252, 251))
	$stringFormat = New-Object System.Drawing.StringFormat
	$stringFormat.Alignment = [System.Drawing.StringAlignment]::Center
	$stringFormat.LineAlignment = [System.Drawing.StringAlignment]::Center
	$graphics.DrawString("NFS", $font, $textBrush, [System.Drawing.RectangleF]::new(54, 96, 148, 72), $stringFormat)

	$bitmap.Save($pngPath, [System.Drawing.Imaging.ImageFormat]::Png)

	$memory = New-Object System.IO.MemoryStream
	try {
		$bitmap.Save($memory, [System.Drawing.Imaging.ImageFormat]::Png)
		Write-IconFromPngBytes -PngBytes $memory.ToArray() -Path $icoPath
	} finally {
		$memory.Dispose()
	}
} finally {
	$graphics.Dispose()
	$bitmap.Dispose()
}

Write-Host "Icon created: $icoPath"
