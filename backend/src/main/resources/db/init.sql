create table if not exists users (
    username text primary key,
    password_hash text not null,
    display_name text not null,
    role text not null
);

create table if not exists products (
    code text primary key,
    barcode text not null,
    name text not null,
    category text,
    unit text,
    safety_stock integer not null,
    created_at text not null,
    image_url text
);

create table if not exists inventory (
    product_code text primary key,
    code text not null unique,
    quantity integer not null,
    last_changed_at text not null
);

create table if not exists stock_movements (
    id integer primary key autoincrement,
    type text not null,
    product_code text not null,
    product_name text not null,
    quantity integer not null,
    before_quantity integer not null,
    after_quantity integer not null,
    operator text,
    remark text,
    customer_code text,
    created_at text not null
);

create table if not exists customers (
    code text primary key,
    name text not null,
    contact_person text,
    phone text not null,
    qq text,
    wechat text,
    address text,
    remark text,
    created_at text not null
);

create table if not exists dict_categories (
    id integer primary key autoincrement,
    name text not null unique
);

create table if not exists dict_items (
    id integer primary key autoincrement,
    category text not null,
    label text not null,
    value text not null
);

insert or ignore into users (username, password_hash, display_name, role)
values
    ('admin', 'e82c6e48905a96d5596fac29c59bd5a90c6092dfe73f4cfdde257165ae9cd40c', '系统管理员', '老板'),
    ('warehouse', '6a00501949f5241cb39097f540bca10c7f31f626208651d9c9e94703fa0e18d1', '仓管员', '仓管'),
    ('sales', '7a60ec6719fc1d766c178275fd98d3dddb877df81315bfb68e481da6dbf4f343', '销售员', '销售');

insert or ignore into dict_categories (name)
values
    ('商品分类'),
    ('商品单位'),
    ('备注模板');

insert or ignore into dict_items (category, label, value)
values
    ('商品分类', '五金件', '五金件'),
    ('商品分类', '辅料', '辅料'),
    ('商品分类', '半成品', '半成品'),
    ('商品分类', '包材', '包材'),
    ('商品单位', '件', '件'),
    ('商品单位', '盒', '盒'),
    ('商品单位', '包', '包'),
    ('商品单位', '个', '个'),
    ('备注模板', '正常入库', '正常入库'),
    ('备注模板', '正常出库', '正常出库'),
    ('备注模板', '库存调整', '库存调整');
