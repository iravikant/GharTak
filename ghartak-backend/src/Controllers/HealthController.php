<?php

declare(strict_types=1);

namespace App\Controllers;

use App\Http\Request;
use App\Http\Response;

final class HealthController
{
    public function check(Request $request): void
    {
        Response::success(['status' => 'healthy'], 'OK');
    }
}
