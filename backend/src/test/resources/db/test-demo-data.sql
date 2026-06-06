insert into products (code, barcode, name, category, unit, safety_stock, created_at, image_url)
values
    ('P-1001', '6900000000017', '不锈钢螺丝 M6', '五金件', '盒', 20, '2026-01-01T00:00:00+08:00', null),
    ('P-1002', '6900000000024', '尼龙扎带 200mm', '辅料', '包', 10, '2026-01-01T00:00:00+08:00', null),
    ('P-1003', '6900000000031', '铝合金外壳 A 型', '半成品', '件', 12, '2026-01-01T00:00:00+08:00', null);

insert into inventory (product_code, code, quantity, last_changed_at)
values
    ('P-1001', 'I-1001', 120, '2026-01-01T00:00:00+08:00'),
    ('P-1002', 'I-1002', 64, '2026-01-01T00:00:00+08:00'),
    ('P-1003', 'I-1003', 18, '2026-01-01T00:00:00+08:00');
