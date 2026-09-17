import os
from PIL import Image, ImageDraw

def create_icons():
    base_dir = r"c:\Users\User\Documents\PRECIOSO\Code\Projetos_pessoais\Vira"
    branding_dir = os.path.join(base_dir, "branding")
    res_dir = os.path.join(base_dir, "app", "src", "main", "res")

    sym_black_path = os.path.join(branding_dir, "symbol_black.png")
    sym_white_path = os.path.join(branding_dir, "symbol_white.png")

    sym_black = Image.open(sym_black_path).convert("RGBA")
    sym_white = Image.open(sym_white_path).convert("RGBA")

    cyan_color = (0, 209, 178, 255) # #00D1B2

    densities = {
        "mipmap-mdpi": (108, 48),
        "mipmap-hdpi": (162, 72),
        "mipmap-xhdpi": (216, 96),
        "mipmap-xxhdpi": (324, 144),
        "mipmap-xxxhdpi": (432, 192),
    }

    for folder, (adapt_size, leg_size) in densities.items():
        out_dir = os.path.join(res_dir, folder)
        os.makedirs(out_dir, exist_ok=True)

        # 1. Adaptive Foreground (black symbol on transparent canvas)
        fg_canvas = Image.new("RGBA", (adapt_size, adapt_size), (0, 0, 0, 0))
        # Target size in safe zone: 58% of canvas size
        target_size = int(adapt_size * 0.58)
        # maintain aspect ratio of symbol
        ratio = min(target_size / sym_black.width, target_size / sym_black.height)
        new_w = int(sym_black.width * ratio)
        new_h = int(sym_black.height * ratio)
        resized_sym_black = sym_black.resize((new_w, new_h), Image.Resampling.LANCZOS)
        offset_x = (adapt_size - new_w) // 2
        offset_y = (adapt_size - new_h) // 2
        fg_canvas.paste(resized_sym_black, (offset_x, offset_y), resized_sym_black)
        fg_canvas.save(os.path.join(out_dir, "ic_launcher_foreground.png"), "PNG")

        # 2. Monochrome Icon (white symbol on transparent canvas)
        mono_canvas = Image.new("RGBA", (adapt_size, adapt_size), (0, 0, 0, 0))
        resized_sym_white = sym_white.resize((new_w, new_h), Image.Resampling.LANCZOS)
        mono_canvas.paste(resized_sym_white, (offset_x, offset_y), resized_sym_white)
        mono_canvas.save(os.path.join(out_dir, "ic_launcher_monochrome.png"), "PNG")

        # 3. Legacy Square Icon (rounded rectangle)
        leg_canvas = Image.new("RGBA", (leg_size, leg_size), (0, 0, 0, 0))
        mask_sq = Image.new("L", (leg_size, leg_size), 0)
        draw_sq = ImageDraw.Draw(mask_sq)
        corner_r = int(leg_size * 0.22)
        draw_sq.rounded_rectangle([0, 0, leg_size - 1, leg_size - 1], radius=corner_r, fill=255)
        bg_sq = Image.new("RGBA", (leg_size, leg_size), cyan_color)
        leg_canvas.paste(bg_sq, (0, 0), mask_sq)
        
        target_leg = int(leg_size * 0.62)
        ratio_leg = min(target_leg / sym_black.width, target_leg / sym_black.height)
        leg_w = int(sym_black.width * ratio_leg)
        leg_h = int(sym_black.height * ratio_leg)
        resized_leg_black = sym_black.resize((leg_w, leg_h), Image.Resampling.LANCZOS)
        leg_canvas.paste(resized_leg_black, ((leg_size - leg_w) // 2, (leg_size - leg_h) // 2), resized_leg_black)
        leg_canvas.save(os.path.join(out_dir, "ic_launcher.png"), "PNG")

        # 4. Legacy Round Icon (circle)
        round_canvas = Image.new("RGBA", (leg_size, leg_size), (0, 0, 0, 0))
        mask_rd = Image.new("L", (leg_size, leg_size), 0)
        draw_rd = ImageDraw.Draw(mask_rd)
        draw_rd.ellipse([0, 0, leg_size - 1, leg_size - 1], fill=255)
        bg_rd = Image.new("RGBA", (leg_size, leg_size), cyan_color)
        round_canvas.paste(bg_rd, (0, 0), mask_rd)
        round_canvas.paste(resized_leg_black, ((leg_size - leg_w) // 2, (leg_size - leg_h) // 2), resized_leg_black)
        round_canvas.save(os.path.join(out_dir, "ic_launcher_round.png"), "PNG")

        # Remove old webp if present
        for old_file in ["ic_launcher.webp", "ic_launcher_round.webp"]:
            p = os.path.join(out_dir, old_file)
            if os.path.exists(p):
                os.remove(p)

    print("Icons generated successfully!")

if __name__ == "__main__":
    create_icons()
