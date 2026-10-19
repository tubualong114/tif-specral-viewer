# QuPath 像素光谱查看器

面向 **QuPath 0.7.0** 的开源扩展，用于浏览多通道图像并读取单个像素的原始逐通道强度。

[English documentation](README_en.md)

## 功能

- 通道滑条：`0` 显示全部直接通道，`1…N` 显示指定单通道。
- 光谱探针：启用后在主图像中单击像素，读取当前 Z/T 平面的全部原始通道值。
- 非模态光谱窗口：显示坐标、像素类型、光谱曲线和逐通道数据表。
- 中英文界面：跟随 Java/QuPath 运行区域设置。
- 后台读取：快速连续点击只展示最后一次请求的结果，不阻塞 JavaFX 界面。

插件显示的是原始像素强度，不受 QuPath 的 LUT、亮度或对比度设置影响。

## 安装

1. 从项目 Release 或本地构建结果中取得 `qupath-extension-spectrum-viewer-*.jar`。
2. 启动 QuPath 0.7.0，将 JAR 拖入 QuPath 主窗口并选择用户目录。
3. 重启 QuPath。
4. 打开 `Extensions > 像素光谱查看器 > 打开像素光谱查看器`。

打开窗口时会自动切换到“光谱探针”。单击工具栏中的移动工具即可恢复拖动画布；窗口中的“启用探针”按钮可再次进入取样模式。

## 样例 TIFF

仓库 `data/` 中的文件是 ImageJ TIFF，维度分别为：

| 文件 | 维度 | 类型 |
| --- | --- | --- |
| `AF488_L445.tif` | 28 × 2048 × 2048（CYX） | uint8 |
| `AF647_L488.tif` | 23 × 2048 × 2048（CYX） | uint8 |
| `ATTO620_L514.tif` | 20 × 2048 × 2048（CYX） | uint8 |

这些文件没有真实波长元数据，因此插件使用 QuPath 通道名称，并在名称缺失时回退为 `Channel 1…N`。如果 QuPath 没有把 TIFF 识别为上述通道数，请在打开图像时明确选择 **Bio-Formats** reader。

## 构建

需要能够运行 Gradle 的 JDK；QuPath 0.7.0 官方建议 OpenJDK 25。项目自带 Gradle Wrapper，不要求全局安装 Gradle。

Windows PowerShell：

```powershell
./gradlew.bat clean test build
```

macOS/Linux：

```bash
./gradlew clean test build
```

生成的扩展 JAR 位于 `build/libs/`。QuPath、JavaFX 和日志依赖不会打入普通插件 JAR。

## 验证像素值

可使用 Python `tifffile` 对固定坐标做独立核验：

```python
import tifffile

image = tifffile.imread("data/AF488_L445.tif")
x, y = 100, 100
print(image[:, y, x])
```

输出应与插件在 `x=100, y=100, z=0, t=0` 显示的逐通道值一致。

## 范围限制

- 第一版只读取单个像素，不进行邻域或 ROI 平均。
- 不从文件名推断波长，也不执行光谱解混。
- 图像解码由 QuPath 的 `ImageServer`（通常为 Bio-Formats）负责。

## 许可证

本项目采用 [Apache License 2.0](LICENSE)。QuPath 本身采用 GPL-3.0-or-later；本扩展不包含 QuPath 的二进制依赖。
