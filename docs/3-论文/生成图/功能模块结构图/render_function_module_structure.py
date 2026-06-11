# -*- coding: utf-8 -*-
from __future__ import annotations

import html
import math
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont


OUT_DIR = Path(__file__).resolve().parent
SVG_PATH = OUT_DIR / "function-module-structure.svg"
PNG_PATH = OUT_DIR / "function-module-structure.png"
PUML_PATH = OUT_DIR / "function-module-structure.puml"

WIDTH = 1900
HEIGHT = 1250
SCALE = 2
FONT_PT = 10.5
FONT_PX = round(FONT_PT * 96 / 72 * SCALE)

BLACK = "black"
WHITE = "white"

SIMSUN = "C:/Windows/Fonts/simsun.ttc"
TIMES = "C:/Windows/Fonts/times.ttf"
TIMES_BOLD = "C:/Windows/Fonts/timesbd.ttf"


def load_font(path: str, size: int) -> ImageFont.FreeTypeFont:
    try:
        return ImageFont.truetype(path, size)
    except OSError:
        return ImageFont.load_default()


CN_FONT = load_font(SIMSUN, FONT_PX)
EN_FONT = load_font(TIMES, FONT_PX)
CN_FONT_SMALL = load_font(SIMSUN, round(FONT_PX * 0.94))
EN_FONT_SMALL = load_font(TIMES, round(FONT_PX * 0.94))


def is_ascii(ch: str) -> bool:
    return ord(ch) < 128


def text_runs(text: str) -> list[tuple[str, bool]]:
    runs: list[tuple[str, bool]] = []
    current = ""
    current_ascii: bool | None = None
    for ch in text:
        ascii_part = is_ascii(ch)
        if current_ascii is None or ascii_part == current_ascii:
            current += ch
            current_ascii = ascii_part
        else:
            runs.append((current, bool(current_ascii)))
            current = ch
            current_ascii = ascii_part
    if current:
        runs.append((current, bool(current_ascii)))
    return runs


def svg_attrs(attrs: dict[str, str | float | int | None]) -> str:
    parts = []
    for key, value in attrs.items():
        if value is None:
            continue
        parts.append(f'{key}="{html.escape(str(value), quote=True)}"')
    return " ".join(parts)


class Svg:
    def __init__(self) -> None:
        self.parts: list[str] = []

    def add(self, raw: str) -> None:
        self.parts.append(raw)

    def rect(self, x: int, y: int, w: int, h: int, dash: bool = False) -> None:
        dash_attr = ' stroke-dasharray="7 5"' if dash else ""
        self.add(
            f'<rect x="{x}" y="{y}" width="{w}" height="{h}" fill="{WHITE}" '
            f'stroke="{BLACK}" stroke-width="1.6"{dash_attr}/>'
        )

    def line(self, points: list[tuple[int, int]], dashed: bool = False, arrow: bool = True) -> None:
        dash_attr = ' stroke-dasharray="8 6"' if dashed else ""
        arrow_attr = ' marker-end="url(#arrow)"' if arrow else ""
        pts = " ".join(f"{x},{y}" for x, y in points)
        self.add(
            f'<polyline points="{pts}" fill="none" stroke="{BLACK}" '
            f'stroke-width="1.8"{dash_attr}{arrow_attr}/>'
        )

    def component(self, x: int, y: int, w: int, h: int, lines: list[str]) -> None:
        self.rect(x, y, w, h)
        icon_x = x + w - 32
        icon_y = y + 10
        self.add(f'<rect x="{icon_x}" y="{icon_y}" width="22" height="16" fill="{WHITE}" stroke="{BLACK}" stroke-width="1.2"/>')
        self.add(f'<rect x="{icon_x - 5}" y="{icon_y + 3}" width="8" height="4" fill="{WHITE}" stroke="{BLACK}" stroke-width="1.1"/>')
        self.add(f'<rect x="{icon_x - 5}" y="{icon_y + 10}" width="8" height="4" fill="{WHITE}" stroke="{BLACK}" stroke-width="1.1"/>')
        center_lines(x + w / 2, y + h / 2, lines, self)

    def database(self, x: int, y: int, w: int, h: int, lines: list[str]) -> None:
        rx = w / 2
        self.add(f'<path d="M{x},{y + 18} C{x},{y - 6} {x + w},{y - 6} {x + w},{y + 18} L{x + w},{y + h - 18} C{x + w},{y + h + 6} {x},{y + h + 6} {x},{y + h - 18} Z" fill="{WHITE}" stroke="{BLACK}" stroke-width="1.6"/>')
        self.add(f'<ellipse cx="{x + rx}" cy="{y + 18}" rx="{rx}" ry="18" fill="{WHITE}" stroke="{BLACK}" stroke-width="1.6"/>')
        center_lines(x + w / 2, y + h / 2 + 6, lines, self)


def svg_text(x: float, y: float, text: str, svg: Svg, anchor: str = "middle", size_pt: float = FONT_PT) -> None:
    attrs = svg_attrs(
        {
            "x": round(x, 2),
            "y": round(y, 2),
            "text-anchor": anchor,
            "dominant-baseline": "middle",
            "font-size": f"{size_pt}pt",
            "fill": BLACK,
        }
    )
    tspans = []
    for run, ascii_part in text_runs(text):
        family = "Times New Roman" if ascii_part else "SimSun, 宋体"
        tspans.append(f'<tspan font-family="{family}">{html.escape(run)}</tspan>')
    svg.add(f"<text {attrs}>{''.join(tspans)}</text>")


def center_lines(cx: float, cy: float, lines: list[str], svg: Svg) -> None:
    line_h = 22
    start = cy - (len(lines) - 1) * line_h / 2
    for index, line in enumerate(lines):
        svg_text(cx, start + index * line_h, line, svg)


def svg_package(svg: Svg, x: int, y: int, w: int, h: int, title: str) -> None:
    tab_w = min(360, max(180, len(title) * 18))
    svg.add(f'<path d="M{x},{y + 24} L{x},{y + h} L{x + w},{y + h} L{x + w},{y + 24} L{x + tab_w},{y + 24} L{x + tab_w - 18},{y} L{x},{y} Z" fill="{WHITE}" stroke="{BLACK}" stroke-width="1.8"/>')
    svg_text(x + 18, y + 13, title, svg, anchor="start")


def line_width(text: str, small: bool = False) -> float:
    width = 0.0
    for run, ascii_part in text_runs(text):
        font = (EN_FONT_SMALL if small else EN_FONT) if ascii_part else (CN_FONT_SMALL if small else CN_FONT)
        width += font.getlength(run)
    return width


def draw_mixed_text(draw: ImageDraw.ImageDraw, x: float, y: float, text: str, anchor: str = "mm", small: bool = False) -> None:
    fonts = {
        (True, False): EN_FONT,
        (False, False): CN_FONT,
        (True, True): EN_FONT_SMALL,
        (False, True): CN_FONT_SMALL,
    }
    runs = text_runs(text)
    width = sum(fonts[(ascii_part, small)].getlength(run) for run, ascii_part in runs)
    horizontal_anchor = anchor[0] if anchor else "m"
    vertical_anchor = anchor[1] if len(anchor) > 1 else "m"
    if horizontal_anchor == "m":
        start_x = x - width / 2
    elif horizontal_anchor == "r":
        start_x = x - width
    else:
        start_x = x
    size_font = CN_FONT_SMALL if small else CN_FONT
    bbox = size_font.getbbox("国")
    height = bbox[3] - bbox[1]
    if vertical_anchor == "m":
        draw_y = y - height / 2
    else:
        draw_y = y
    cursor = start_x
    for run, ascii_part in runs:
        font = fonts[(ascii_part, small)]
        draw.text((cursor, draw_y), run, font=font, fill=BLACK)
        cursor += font.getlength(run)


def draw_center_lines(draw: ImageDraw.ImageDraw, cx: float, cy: float, lines: list[str], small: bool = False) -> None:
    line_h = 22 * SCALE
    start = cy - (len(lines) - 1) * line_h / 2
    for index, line in enumerate(lines):
        draw_mixed_text(draw, cx, start + index * line_h, line, small=small)


def draw_rect(draw: ImageDraw.ImageDraw, x: int, y: int, w: int, h: int, dash: bool = False) -> None:
    box = [x * SCALE, y * SCALE, (x + w) * SCALE, (y + h) * SCALE]
    if not dash:
        draw.rectangle(box, fill=WHITE, outline=BLACK, width=3)
        return
    draw_dashed_line(draw, [(x, y), (x + w, y), (x + w, y + h), (x, y + h), (x, y)], closed=True)


def draw_component(draw: ImageDraw.ImageDraw, x: int, y: int, w: int, h: int, lines: list[str]) -> None:
    draw_rect(draw, x, y, w, h)
    icon_x = (x + w - 32) * SCALE
    icon_y = (y + 10) * SCALE
    draw.rectangle([icon_x, icon_y, icon_x + 22 * SCALE, icon_y + 16 * SCALE], fill=WHITE, outline=BLACK, width=2)
    draw.rectangle([icon_x - 5 * SCALE, icon_y + 3 * SCALE, icon_x + 3 * SCALE, icon_y + 7 * SCALE], fill=WHITE, outline=BLACK, width=2)
    draw.rectangle([icon_x - 5 * SCALE, icon_y + 10 * SCALE, icon_x + 3 * SCALE, icon_y + 14 * SCALE], fill=WHITE, outline=BLACK, width=2)
    draw_center_lines(draw, (x + w / 2) * SCALE, (y + h / 2) * SCALE, lines)


def draw_database(draw: ImageDraw.ImageDraw, x: int, y: int, w: int, h: int, lines: list[str]) -> None:
    sx, sy, sw, sh = x * SCALE, y * SCALE, w * SCALE, h * SCALE
    draw.rectangle([sx, sy + 18 * SCALE, sx + sw, sy + sh - 18 * SCALE], fill=WHITE)
    draw.ellipse([sx, sy, sx + sw, sy + 36 * SCALE], fill=WHITE, outline=BLACK, width=3)
    draw.line([sx, sy + 18 * SCALE, sx, sy + sh - 18 * SCALE], fill=BLACK, width=3)
    draw.line([sx + sw, sy + 18 * SCALE, sx + sw, sy + sh - 18 * SCALE], fill=BLACK, width=3)
    draw.arc([sx, sy + sh - 36 * SCALE, sx + sw, sy + sh], start=0, end=180, fill=BLACK, width=3)
    draw_center_lines(draw, sx + sw / 2, sy + sh / 2 + 6 * SCALE, lines)


def draw_package(draw: ImageDraw.ImageDraw, x: int, y: int, w: int, h: int, title: str) -> None:
    sx, sy, sw, sh = x * SCALE, y * SCALE, w * SCALE, h * SCALE
    tab_w = min(360, max(180, len(title) * 18)) * SCALE
    points = [
        (sx, sy + 24 * SCALE),
        (sx, sy + sh),
        (sx + sw, sy + sh),
        (sx + sw, sy + 24 * SCALE),
        (sx + tab_w, sy + 24 * SCALE),
        (sx + tab_w - 18 * SCALE, sy),
        (sx, sy),
    ]
    draw.polygon(points, fill=WHITE, outline=BLACK)
    draw.line(points + [points[0]], fill=BLACK, width=3)
    draw_mixed_text(draw, sx + 18 * SCALE, sy + 13 * SCALE, title, anchor="lm")


def draw_dashed_line(draw: ImageDraw.ImageDraw, points: list[tuple[int, int]], closed: bool = False) -> None:
    del closed
    scaled = [(x * SCALE, y * SCALE) for x, y in points]
    dash_len = 14
    gap_len = 10
    for (x1, y1), (x2, y2) in zip(scaled, scaled[1:]):
        dx = x2 - x1
        dy = y2 - y1
        dist = math.hypot(dx, dy)
        if dist == 0:
            continue
        ux = dx / dist
        uy = dy / dist
        cursor = 0.0
        while cursor < dist:
            end = min(cursor + dash_len, dist)
            draw.line(
                [x1 + ux * cursor, y1 + uy * cursor, x1 + ux * end, y1 + uy * end],
                fill=BLACK,
                width=3,
            )
            cursor += dash_len + gap_len


def draw_arrow(draw: ImageDraw.ImageDraw, points: list[tuple[int, int]], dashed: bool = False) -> None:
    if dashed:
        draw_dashed_line(draw, points)
    else:
        draw.line([(x * SCALE, y * SCALE) for x, y in points], fill=BLACK, width=4, joint="curve")
    x1, y1 = points[-2]
    x2, y2 = points[-1]
    angle = math.atan2((y2 - y1), (x2 - x1))
    size = 13 * SCALE
    end = (x2 * SCALE, y2 * SCALE)
    left = (
        end[0] - size * math.cos(angle - math.pi / 6),
        end[1] - size * math.sin(angle - math.pi / 6),
    )
    right = (
        end[0] - size * math.cos(angle + math.pi / 6),
        end[1] - size * math.sin(angle + math.pi / 6),
    )
    draw.polygon([end, left, right], fill=BLACK)


def build_svg() -> str:
    svg = Svg()
    svg.add(
        f'<svg xmlns="http://www.w3.org/2000/svg" width="{WIDTH}" height="{HEIGHT}" '
        f'viewBox="0 0 {WIDTH} {HEIGHT}">'
    )
    svg.add("<title>超市库存管理系统功能模块结构图</title>")
    svg.add("<desc>黑白 UML 组件图；中文字体为五号宋体，英文和数字为 5 号 Times New Roman。</desc>")
    svg.add(
        """
<defs>
  <marker id="arrow" markerWidth="10" markerHeight="10" refX="9" refY="5" orient="auto" markerUnits="strokeWidth">
    <path d="M0,0 L10,5 L0,10 Z" fill="black"/>
  </marker>
</defs>
"""
    )
    svg.add(f'<rect x="0" y="0" width="{WIDTH}" height="{HEIGHT}" fill="{WHITE}"/>')
    svg_text(WIDTH / 2, 36, "超市库存管理系统功能模块结构图（UML组件图）", svg)

    svg_package(svg, 70, 70, 1760, 235, "表现层：Vue 3 管理后台")
    svg.component(105, 118, 280, 125, ["<<component>>", "前端应用", "Router / Pinia / Axios"])
    front_boxes = [
        (455, 112, "登录认证"),
        (625, 112, "首页工作台"),
        (795, 112, "用户权限"),
        (965, 112, "商品中心"),
        (1135, 112, "供应商管理"),
        (1305, 112, "采购入库"),
        (1475, 112, "库存中心"),
        (545, 202, "出库管理"),
        (715, 202, "盘点管理"),
        (885, 202, "报表统计"),
        (1055, 202, "系统信息"),
    ]
    for x, y, label in front_boxes:
        svg.component(x, y, 145, 48, ["<<component>>", label])

    svg_package(svg, 70, 365, 1760, 565, "应用服务层：Spring Boot REST API")
    svg.component(110, 415, 295, 82, ["<<component>>", "认证与权限模块", "Auth / User / Role / Permission"])
    svg.component(1435, 415, 300, 82, ["<<component>>", "公共支撑模块", "Response / Exception / Page"])
    svg.component(110, 560, 250, 88, ["<<component>>", "主数据模块", "Category / Brand / SPU", "SKU / Unit"])
    svg.component(405, 560, 250, 88, ["<<component>>", "供应商模块", "Supplier / SupplierSku"])
    svg.component(700, 560, 275, 88, ["<<component>>", "采购入库模块", "Plan / Approval / Receipt"])
    svg.component(1030, 560, 250, 88, ["<<component>>", "库存总账模块", "Stock / StockLog"])
    svg.component(1385, 560, 300, 88, ["<<component>>", "库存批次模块", "StockBatch / BatchLog"])
    svg.component(700, 740, 250, 88, ["<<component>>", "出库管理模块", "Outbound / FEFO"])
    svg.component(1030, 740, 250, 88, ["<<component>>", "盘点管理模块", "StockCheck"])
    svg.component(1385, 740, 300, 88, ["<<component>>", "报表统计模块", "Report"])
    svg.component(405, 740, 250, 88, ["<<component>>", "系统信息模块", "System"])
    svg_text(925, 894, "Controller -> Service -> Mapper -> MySQL", svg)

    svg_package(svg, 70, 1010, 1760, 170, "数据层：MySQL 8.0")
    svg.database(105, 1050, 260, 95, ["<<database>>", "业务数据库", "market"])
    table_boxes = [
        (425, 1048, "用户/角色/权限表"),
        (655, 1048, "分类/品牌/SPU/SKU表"),
        (910, 1048, "供应商/供货绑定表"),
        (1165, 1048, "采购/收货单据表"),
        (1420, 1048, "库存/批次/流水表"),
        (655, 1112, "出库/盘点/报表数据"),
    ]
    for x, y, label in table_boxes:
        svg.rect(x, y, 210, 43)
        svg_text(x + 105, y + 22, label, svg)

    # Inter-layer dependencies
    svg.line([(245, 243), (245, 365)], dashed=False)
    svg.line([(925, 930), (925, 1010)], dashed=False)

    # Cross-cutting support
    svg.line([(258, 497), (258, 560)], dashed=True)
    svg.line([(1585, 497), (1585, 540)], dashed=True)

    # Business module dependencies
    svg.line([(360, 604), (405, 604)])
    svg.line([(655, 604), (700, 604)])
    svg.line([(975, 604), (1030, 604)])
    svg.line([(1280, 604), (1385, 604)])
    svg.line([(825, 740), (825, 690), (1155, 690), (1155, 648)])
    svg.line([(1155, 740), (1155, 648)])
    svg.line([(1280, 784), (1385, 648)])
    svg.line([(1385, 784), (1280, 784)], dashed=True)
    svg.line([(825, 648), (825, 740)], dashed=True)

    # Data access dependencies
    svg.add("</svg>")
    return "\n".join(svg.parts)


def draw_png() -> None:
    image = Image.new("RGB", (WIDTH * SCALE, HEIGHT * SCALE), WHITE)
    draw = ImageDraw.Draw(image)
    draw_mixed_text(draw, WIDTH * SCALE / 2, 36 * SCALE, "超市库存管理系统功能模块结构图（UML组件图）")

    draw_package(draw, 70, 70, 1760, 235, "表现层：Vue 3 管理后台")
    draw_component(draw, 105, 118, 280, 125, ["<<component>>", "前端应用", "Router / Pinia / Axios"])
    for x, y, label in [
        (455, 112, "登录认证"),
        (625, 112, "首页工作台"),
        (795, 112, "用户权限"),
        (965, 112, "商品中心"),
        (1135, 112, "供应商管理"),
        (1305, 112, "采购入库"),
        (1475, 112, "库存中心"),
        (545, 202, "出库管理"),
        (715, 202, "盘点管理"),
        (885, 202, "报表统计"),
        (1055, 202, "系统信息"),
    ]:
        draw_component(draw, x, y, 145, 48, ["<<component>>", label])

    draw_package(draw, 70, 365, 1760, 565, "应用服务层：Spring Boot REST API")
    draw_component(draw, 110, 415, 295, 82, ["<<component>>", "认证与权限模块", "Auth / User / Role / Permission"])
    draw_component(draw, 1435, 415, 300, 82, ["<<component>>", "公共支撑模块", "Response / Exception / Page"])
    draw_component(draw, 110, 560, 250, 88, ["<<component>>", "主数据模块", "Category / Brand / SPU", "SKU / Unit"])
    draw_component(draw, 405, 560, 250, 88, ["<<component>>", "供应商模块", "Supplier / SupplierSku"])
    draw_component(draw, 700, 560, 275, 88, ["<<component>>", "采购入库模块", "Plan / Approval / Receipt"])
    draw_component(draw, 1030, 560, 250, 88, ["<<component>>", "库存总账模块", "Stock / StockLog"])
    draw_component(draw, 1385, 560, 300, 88, ["<<component>>", "库存批次模块", "StockBatch / BatchLog"])
    draw_component(draw, 700, 740, 250, 88, ["<<component>>", "出库管理模块", "Outbound / FEFO"])
    draw_component(draw, 1030, 740, 250, 88, ["<<component>>", "盘点管理模块", "StockCheck"])
    draw_component(draw, 1385, 740, 300, 88, ["<<component>>", "报表统计模块", "Report"])
    draw_component(draw, 405, 740, 250, 88, ["<<component>>", "系统信息模块", "System"])
    draw_mixed_text(draw, 925 * SCALE, 894 * SCALE, "Controller -> Service -> Mapper -> MySQL")

    draw_package(draw, 70, 1010, 1760, 170, "数据层：MySQL 8.0")
    draw_database(draw, 105, 1050, 260, 95, ["<<database>>", "业务数据库", "market"])
    for x, y, label in [
        (425, 1048, "用户/角色/权限表"),
        (655, 1048, "分类/品牌/SPU/SKU表"),
        (910, 1048, "供应商/供货绑定表"),
        (1165, 1048, "采购/收货单据表"),
        (1420, 1048, "库存/批次/流水表"),
        (655, 1112, "出库/盘点/报表数据"),
    ]:
        draw_rect(draw, x, y, 210, 43)
        draw_mixed_text(draw, (x + 105) * SCALE, (y + 22) * SCALE, label)

    for points, dashed in [
        ([(245, 243), (245, 365)], False),
        ([(925, 930), (925, 1010)], False),
        ([(258, 497), (258, 560)], True),
        ([(1585, 497), (1585, 540)], True),
        ([(360, 604), (405, 604)], False),
        ([(655, 604), (700, 604)], False),
        ([(975, 604), (1030, 604)], False),
        ([(1280, 604), (1385, 604)], False),
        ([(825, 740), (825, 690), (1155, 690), (1155, 648)], False),
        ([(1155, 740), (1155, 648)], False),
        ([(1280, 784), (1385, 648)], False),
        ([(1385, 784), (1280, 784)], True),
        ([(825, 648), (825, 740)], True),
    ]:
        draw_arrow(draw, points, dashed=dashed)

    image.save(PNG_PATH)


def build_puml() -> str:
    return """@startuml
' 超市库存管理系统功能模块结构图
skinparam monochrome true
skinparam shadowing false
skinparam componentStyle uml2
skinparam defaultFontName "SimSun"
skinparam packageStyle rectangle
left to right direction

actor "管理员/普通用户" as Actor

package "表现层：Vue 3 管理后台" {
  [前端应用\\nRouter / Pinia / Axios] as Frontend <<component>>
  [登录认证] as FLogin <<component>>
  [首页工作台] as FDashboard <<component>>
  [用户权限] as FUser <<component>>
  [商品中心] as FProduct <<component>>
  [供应商管理] as FSupplier <<component>>
  [采购入库] as FPurchase <<component>>
  [库存中心] as FStock <<component>>
  [出库管理] as FOutbound <<component>>
  [盘点管理] as FCheck <<component>>
  [报表统计] as FReport <<component>>
  [系统信息] as FSystem <<component>>
}

package "应用服务层：Spring Boot REST API" {
  [认证与权限模块\\nAuth / User / Role / Permission] as Auth <<component>>
  [公共支撑模块\\nResponse / Exception / Page] as Common <<component>>
  [主数据模块\\nCategory / Brand / SPU / SKU / Unit] as Master <<component>>
  [供应商模块\\nSupplier / SupplierSku] as Supplier <<component>>
  [采购入库模块\\nPlan / Approval / Receipt] as Purchase <<component>>
  [库存总账模块\\nStock / StockLog] as Stock <<component>>
  [库存批次模块\\nStockBatch / BatchLog] as Batch <<component>>
  [出库管理模块\\nOutbound / FEFO] as Outbound <<component>>
  [盘点管理模块\\nStockCheck] as Check <<component>>
  [报表统计模块\\nReport] as Report <<component>>
  [系统信息模块\\nSystem] as System <<component>>
}

database "数据层：MySQL 8.0\\n业务数据库 market" as DB

Actor --> Frontend
Frontend --> Auth : REST API
Frontend --> Master
Frontend --> Supplier
Frontend --> Purchase
Frontend --> Stock
Frontend --> Outbound
Frontend --> Check
Frontend --> Report
Frontend --> System

Auth ..> Master : 鉴权/权限
Common ..> Master : 统一响应/异常/分页
Master --> Supplier
Supplier --> Purchase
Purchase --> Stock
Stock --> Batch
Outbound --> Stock
Check --> Stock
Check --> Batch
Report ..> Purchase : 只读统计
Report ..> Outbound : 只读统计
Report ..> Stock : 只读统计
Report ..> Batch : 只读统计

Auth --> DB
Master --> DB
Supplier --> DB
Purchase --> DB
Stock --> DB
Batch --> DB
Outbound --> DB
Check --> DB
Report --> DB
System --> DB
@enduml
"""


def main() -> None:
    SVG_PATH.write_text(build_svg(), encoding="utf-8")
    PUML_PATH.write_text(build_puml(), encoding="utf-8")
    draw_png()
    print(SVG_PATH)
    print(PNG_PATH)
    print(PUML_PATH)


if __name__ == "__main__":
    main()
