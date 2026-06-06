create table users (
    username text primary key,
    password_hash text not null,
    display_name text not null,
    role text not null
);

create table products (
    code text primary key,
    barcode text not null,
    name text not null,
    category text,
    unit text,
    safety_stock integer not null,
    created_at text not null,
    image_url text
);

create table inventory (
    product_code text primary key,
    code text not null unique,
    quantity integer not null,
    last_changed_at text not null
);

create table stock_movements (
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

create table customers (
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

create table dict_categories (
    id integer primary key autoincrement,
    name text not null unique
);

create table dict_items (
    id integer primary key autoincrement,
    category text not null,
    label text not null,
    value text not null
);
