use std::sync::{LazyLock, Mutex};

use crate::client::client::Client;

pub mod account;
pub mod client;
pub mod features;
pub mod java;
pub mod management;
pub mod utils;

pub static CLIENT: LazyLock<Mutex<Client>> = LazyLock::new(|| Mutex::new(Client::default()));
