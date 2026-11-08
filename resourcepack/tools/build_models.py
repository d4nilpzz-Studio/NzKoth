import json
import os
import re
import struct
import sys
import zlib

NAMESPACE = "nzkoth"
BASE_ITEM = "netherite_sword"
BASE_PARENT = "handheld"
FRONT_Z = 7.5
BACK_Z = 8.5
LIGHT_EMISSION = 15
CHANNELS = {0: 1, 2: 3, 3: 1, 4: 2, 6: 4}

DISPLAY = {
    "thirdperson_righthand": {"rotation": [0, -90, 55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
    "thirdperson_lefthand": {"rotation": [0, 90, -55], "translation": [0, 4.0, 0.5], "scale": [0.85, 0.85, 0.85]},
    "firstperson_righthand": {"rotation": [0, -90, 25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "firstperson_lefthand": {"rotation": [0, 90, -25], "translation": [1.13, 3.2, 1.13], "scale": [0.68, 0.68, 0.68]},
    "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.5, 0.5, 0.5]},
    "gui": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
    "head": {"rotation": [0, 0, 0], "translation": [0, 13, 7], "scale": [1, 1, 1]},
    "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [1, 1, 1]},
}


class PngError(Exception):
    pass


def paeth(a, b, c):
    p = a + b - c
    pa, pb, pc = abs(p - a), abs(p - b), abs(p - c)
    if pa <= pb and pa <= pc:
        return a
    return b if pb <= pc else c


def unfilter(raw, width, height, bpp, stride):
    out = bytearray()
    previous = bytearray(stride)
    offset = 0
    for _ in range(height):
        method = raw[offset]
        offset += 1
        line = bytearray(raw[offset:offset + stride])
        offset += stride
        if method == 1:
            for i in range(bpp, stride):
                line[i] = (line[i] + line[i - bpp]) & 255
        elif method == 2:
            for i in range(stride):
                line[i] = (line[i] + previous[i]) & 255
        elif method == 3:
            for i in range(stride):
                left = line[i - bpp] if i >= bpp else 0
                line[i] = (line[i] + ((left + previous[i]) >> 1)) & 255
        elif method == 4:
            for i in range(stride):
                left = line[i - bpp] if i >= bpp else 0
                upleft = previous[i - bpp] if i >= bpp else 0
                line[i] = (line[i] + paeth(left, previous[i], upleft)) & 255
        elif method != 0:
            raise PngError("unsupported filter type %d" % method)
        out.extend(line)
        previous = line
    return out


def alpha_mask(path):
    data = open(path, "rb").read()
    if data[:8] != b"\x89PNG\r\n\x1a\n":
        raise PngError("not a PNG file")
    width = height = depth = color_type = interlace = 0
    idat = bytearray()
    transparent = set()
    palette_alpha = []
    position = 8
    while position < len(data):
        length = struct.unpack(">I", data[position:position + 4])[0]
        tag = data[position + 4:position + 8]
        chunk = data[position + 8:position + 8 + length]
        if tag == b"IHDR":
            width, height, depth, color_type, _, _, interlace = struct.unpack(">IIBBBBB", chunk)
        elif tag == b"IDAT":
            idat.extend(chunk)
        elif tag == b"tRNS":
            if color_type == 3:
                palette_alpha = list(chunk)
            else:
                transparent.add(bytes(chunk))
        position += 12 + length

    if interlace:
        raise PngError("interlaced PNGs are not supported, save it without Adam7")
    if depth != 8:
        raise PngError("bit depth %d is not supported, save it as 8 bits per channel" % depth)
    if color_type not in CHANNELS:
        raise PngError("colour type %d is not supported" % color_type)

    bpp = CHANNELS[color_type]
    stride = width * bpp
    pixels = unfilter(zlib.decompress(bytes(idat)), width, height, bpp, stride)

    rows = []
    for y in range(height):
        row = []
        for x in range(width):
            index = y * stride + x * bpp
            if color_type == 6:
                row.append(pixels[index + 3] > 0)
            elif color_type == 4:
                row.append(pixels[index + 1] > 0)
            elif color_type == 3:
                entry = pixels[index]
                row.append(palette_alpha[entry] > 0 if entry < len(palette_alpha) else True)
            else:
                row.append(True)
        rows.append(row)
    return width, height, rows


def runs_of(rows, y):
    spans = []
    start = None
    width = len(rows[y])
    for x in range(width + 1):
        if x < width and rows[y][x]:
            if start is None:
                start = x
        elif start is not None:
            spans.append((start, x - 1))
            start = None
    return spans


def opaque(rows, x, y):
    if y < 0 or y >= len(rows) or x < 0 or x >= len(rows[y]):
        return False
    return rows[y][x]


def round4(value):
    rounded = round(value, 4)
    return int(rounded) if rounded == int(rounded) else rounded


def build_elements(rows, size):
    scale = 16.0 / size
    elements = []
    for y in range(size):
        for x0, x1 in runs_of(rows, y):
            u0, u1 = round4(x0 * scale), round4((x1 + 1) * scale)
            v0, v1 = round4(y * scale), round4((y + 1) * scale)
            faces = {
                "south": {"uv": [u0, v0, u1, v1], "texture": "#layer0"},
                "north": {"uv": [u1, v0, u0, v1], "texture": "#layer0"},
            }
            if not opaque(rows, x0 - 1, y):
                faces["west"] = {"uv": [u0, v0, round4((x0 + 1) * scale), v1], "texture": "#layer0"}
            if not opaque(rows, x1 + 1, y):
                faces["east"] = {"uv": [round4(x1 * scale), v0, u1, v1], "texture": "#layer0"}
            if any(not opaque(rows, x, y - 1) for x in range(x0, x1 + 1)):
                faces["up"] = {"uv": [u0, v0, u1, v1], "texture": "#layer0"}
            if any(not opaque(rows, x, y + 1) for x in range(x0, x1 + 1)):
                faces["down"] = {"uv": [u0, v0, u1, v1], "texture": "#layer0"}
            elements.append({
                "from": [u0, round4((size - 1 - y) * scale), FRONT_Z],
                "to": [u1, round4((size - y) * scale), BACK_Z],
                "light_emission": LIGHT_EMISSION,
                "faces": faces,
            })
    return elements


def item_model(name, rows, size):
    texture = "%s:item/%s" % (NAMESPACE, name)
    return {
        "textures": {"layer0": texture, "particle": texture},
        "elements": build_elements(rows, size),
        "display": DISPLAY,
    }


def modern_definition(mapping):
    entries = [{
        "threshold": value,
        "model": {"type": "minecraft:model", "model": "%s:item/%s" % (NAMESPACE, name)},
    } for name, value in mapping]
    return {
        "model": {
            "type": "minecraft:range_dispatch",
            "property": "minecraft:custom_model_data",
            "index": 0,
            "fallback": {"type": "minecraft:model", "model": "minecraft:item/" + BASE_ITEM},
            "entries": entries,
        }
    }


def legacy_definition(mapping):
    return {
        "parent": "minecraft:item/" + BASE_PARENT,
        "textures": {"layer0": "minecraft:item/" + BASE_ITEM},
        "overrides": [{
            "predicate": {"custom_model_data": value},
            "model": "%s:item/%s" % (NAMESPACE, name),
        } for name, value in mapping],
    }


def dump(obj):
    text = json.dumps(obj, indent=2)
    return re.sub(r"\[\s+([-\d.,\s]+?)\s+\]",
                  lambda m: "[" + ", ".join(m.group(1).split()).replace(",,", ",") + "]",
                  text)


def write(path, obj):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as handle:
        handle.write(dump(obj) + "\n")


def main():
    tools = os.path.dirname(os.path.abspath(__file__))
    pack = os.path.join(os.path.dirname(tools), "NzKoth")
    textures = os.path.join(pack, "assets", NAMESPACE, "textures", "item")
    models = os.path.join(pack, "assets", NAMESPACE, "models", "item")

    with open(os.path.join(tools, "items.json"), encoding="utf-8") as handle:
        config = json.load(handle)
    mapping = sorted(config.items(), key=lambda pair: pair[1])

    failures = 0
    for name, value in mapping:
        source = os.path.join(textures, name + ".png")
        if not os.path.exists(source):
            print("MISSING  %s.png has no texture in %s" % (name, textures))
            failures += 1
            continue
        try:
            width, height, rows = alpha_mask(source)
        except PngError as error:
            print("BAD PNG  %s.png: %s" % (name, error))
            failures += 1
            continue
        if width != height:
            print("BAD SIZE %s.png is %dx%d, it must be square" % (name, width, height))
            failures += 1
            continue
        model = item_model(name, rows, width)
        write(os.path.join(models, name + ".json"), model)
        print("%-10s cmd %-5d %2dx%-3d %3d elements" % (name, value, width, height, len(model["elements"])))

    write(os.path.join(pack, "assets", "minecraft", "items", BASE_ITEM + ".json"), modern_definition(mapping))
    write(os.path.join(pack, "assets", "minecraft", "models", "item", BASE_ITEM + ".json"), legacy_definition(mapping))
    print("%s mapping rebuilt with %d item(s)" % (BASE_ITEM, len(mapping)))
    return 1 if failures else 0


if __name__ == "__main__":
    sys.exit(main())
