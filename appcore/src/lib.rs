use crate::_client::Client;
use std::sync::{LazyLock, Mutex};

pub mod _client;
pub mod account;
pub mod client;
pub mod features;
pub mod java;
pub mod management;
pub mod utils;

pub static CLIENT: LazyLock<Mutex<Client>> = LazyLock::new(|| Mutex::new(Client::default()));
